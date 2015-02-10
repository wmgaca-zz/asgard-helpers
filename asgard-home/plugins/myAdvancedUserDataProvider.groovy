import groovy.json.JsonSlurper

import com.netflix.asgard.userdata.DefaultUserDataProvider
import com.netflix.asgard.UserContext
import com.netflix.asgard.plugin.AdvancedUserDataProvider
import com.netflix.asgard.model.LaunchContext
import javax.xml.bind.DatatypeConverter
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Cipher
import com.amazonaws.services.ec2.model.Image

import org.apache.zookeeper.ZooKeeper


class MyAdvancedUserDataProvider implements AdvancedUserDataProvider {

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

        static ZooKeeper instance = null

        static getAESKey() {
            System.getenv()['ZK_AES_KEY']
        }

        static getConnString() {
            System.getenv()['ZK_CONN_STRING']
        }

        static ZooKeeper getZooKeeper() {
            if (instance == null) {
                instance = new ZooKeeper(getConnString(), 3000, null)
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

        static String getRaw(DubizzleAppContext appContext) {
            def data = new String(getRawData(appContext))

            def cipher = Cipher.getInstance("AES/ECB/NoPadding")
            SecretKeySpec key = new SecretKeySpec(getAESKey().getBytes('UTF-8'), 'AES')
            cipher.init(Cipher.DECRYPT_MODE, key)

            data = new String(cipher.doFinal(data.decodeBase64()), "UTF-8")

            (new JsonSlurper()).parseText(data)
        }

        static Map getMap(DubizzleAppContext appContext) {
            (new JsonSlurper()).parseText(getRaw(appContext))
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
        def userData = formatMap(ZKHelper.getMap(appContext))
        DatatypeConverter.printBase64Binary(userData.getBytes('UTF-8'))
    }
}

