import groovy.json.JsonSlurper
import com.netflix.asgard.userdata.DefaultUserDataProvider
import com.netflix.asgard.plugin.AdvancedUserDataProvider
import com.netflix.asgard.UserContext
import com.netflix.asgard.model.LaunchContext
import com.amazonaws.services.ec2.model.Image
import javax.xml.bind.DatatypeConverter
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Cipher
import org.apache.zookeeper.ZooKeeper


class DubizzleAdvancedUserDataProvider implements AdvancedUserDataProvider {

    class DubizzleAppContext {

        String name
        String version

        DubizzleAppContext(Image image) {
            name = getAppName(image)
            version = getAppVersion(image)
        }

        private String getAppName(Image image) {
            image.getName().split('-')[0]
        }

        private String getAppVersion(Image image) {
            image.getName().split('-')[1]
        }
    }

    class ZKHelper {

        static final String AES_KEY = System.getenv()['ZK_AES_KEY']

        static final String CONN_STRING = System.getenv()['ZK_CONN_STRING']

        static ZooKeeper instance = null


        static ZooKeeper getZooKeeper() {
            if (instance == null) {
                instance = new ZooKeeper(CONN_STRING, 3000, null)
            }
            instance
        }

        static byte[] getRawData(DubizzleAppContext appContext) {
            ZKHelper.getZooKeeper().getData(
                "/production/docker/${appContext.name}/${appContext.version}/env",
                null,
                null
            )
        }

        static Map getMappedData(DubizzleAppContext appContext) {
            def data = new String(getRawData(appContext))

            def cipher = Cipher.getInstance("AES/ECB/NoPadding")
            SecretKeySpec key = new SecretKeySpec(AES_KEY.getBytes('UTF-8'), 'AES')
            cipher.init(Cipher.DECRYPT_MODE, key)

            // Decrypt & B64 decode
            data = new String(cipher.doFinal(data.decodeBase64()), "UTF-8")

            // Remove trailing "{"
            data = data.replaceAll(/\{+$/, "")

            // We're dealing with a json.dumps(json.dumps(s)) situation over
            // here. It's not producing a JSON that JsonSlurper is capable of
            // understanding.
            //
            // What I'm doing here is bad and I should feel bad, I know.
            // Feeling bad while blaming PySysEnv...
            data = "python ${System.getProperty("user.home")}/.asgard/bin/json ${data}".execute().text

            new JsonSlurper().parseText(data)
        }
    }

    String formatMap(Map data) {
        def arr = []
        for (entry in data) {
            arr.add("export $entry\n")
        }
        arr.join("\n")
    }

    String buildUserData(LaunchContext launchContext) {
        DubizzleAppContext appContext = new DubizzleAppContext(launchContext.image)
        def userData = formatMap(ZKHelper.getMappedData(appContext))
        DatatypeConverter.printBase64Binary(userData.getBytes('UTF-8'))
    }
}

