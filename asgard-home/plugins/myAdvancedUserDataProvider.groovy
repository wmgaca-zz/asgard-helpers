import com.netflix.asgard.userdata.DefaultUserDataProvider
import com.netflix.asgard.UserContext
import com.netflix.asgard.plugin.AdvancedUserDataProvider
import com.netflix.asgard.model.LaunchContext
import javax.xml.bind.DatatypeConverter
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

        static getConnString() {
            System.getenv()['ZK_CONN_STRING']
        }

        static ZooKeeper getZooKeeper() {
            if (instance == null) {
                instance = new ZooKeeper(getConnString(), 3000, null)
            }
            instance
        }
    }

    private byte[] getZKData(DubizzleAppContext appContext) {
        ZKHelper.getZooKeeper().getData(
            "/production/docker/${appContext.name}/${appContext.version}/env",
            null,
            null
        )
    }

    String buildUserData(LaunchContext launchContext) {
        DubizzleAppContext appContext = new DubizzleAppContext(launchContext.image)
        DatatypeConverter.printBase64Binary(getZKData(appContext))
    }
}
