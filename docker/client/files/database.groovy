@Grab('info.picocli:picocli-groovy:4.7.5')
@Grab('org.postgresql:postgresql:42.6.0')
@Command(description="Test database connection.", mixinStandardHelpOptions=true)
@picocli.groovy.PicocliScript
import groovy.transform.Field
import static picocli.CommandLine.*
import org.postgresql.Driver
import java.util.Properties
import java.lang.System
import java.nio.file.Path

@Parameters(arity="1", paramLabel="JDBC_URL", description="JDBC URL for connection.")
@Field String jdbcUrl

@Option(names = ["-u", "--user"], description="Database connection username", required=true)
@Field String username

@Option(names = ["-p", "--password"], description="Database connection password", required=false)
@Field String password

@Option(names = ["-g", "--gss-enc-mode"], description="GssEncMode setting", required=false)
@Field String gssEncMode = "disable"

@Option(names = ["-c", "--krb5-conf"], description="krb5.conf file", required=false)
@Field Path krb5Conf = Path.of("/etc/krb5.conf")

@Option(names = ["-j", "--jaas-conf"], description="jaas.conf file", required=false)
@Field Path jaasConf = Path.of("./jaas.conf")

@Option(names = ["-k", "--keytab"], description="keytab file", required=false)
@Field Path keytab = Path.of("./keytab")

@Option(names = ["-d", "--debug"], description="GSS debug messages", required=false)
@Field boolean debug = false

try {
    if (debug) {
        System.setProperty("sun.security.krb5.debug", "true")
    }
    if (gssEncMode != "disable") {
        if (!keytab.toFile().exists()) {
            println "File $keytab does not exists"
            System.exit(1)
        }
        if (!krb5Conf.toFile().exists()) {
            println "File $krb5Conf does not exists"
            System.exit(1)
        }
        if (!jaasConf.toFile().exists()) {
            println "File $jaasConf does not exists"
            System.exit(1)
        }
        System.setProperty("java.security.krb5.conf", krb5Conf.toAbsolutePath().toString())
        System.setProperty("java.security.auth.login.config", jaasConf.toAbsolutePath().toString())
        System.setProperty("javax.security.auth.useSubjectCredsOnly", "false")
        System.setProperty("jaas.username", username)
        System.setProperty("jaas.keytab", keytab.toAbsolutePath().toString())
    }
    props = new Properties()
    props.setProperty("user", username)
    if (password != null) {
        props.setProperty("password", password)
    }
    if (gssEncMode != null) {
        props.setProperty("gssEncMode", gssEncMode)
    }
    driver = new Driver()
    connection = driver.connect(jdbcUrl, props)
    statement = connection.prepareStatement("SELECT 1")
    try (result = statement.executeQuery()) {
        if (result.next() && result.getInt(1) == 1) {
            println "Success"
        }
    } catch (Exception e2) {
        e.printStackTrace()
        println "Failure"
    }
} catch(Exception e) {
    e.printStackTrace()
    println "Failure"
}
