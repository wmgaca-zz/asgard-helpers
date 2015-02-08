import com.netflix.asgard.userdata.DefaultUserDataProvider
import com.netflix.asgard.UserContext
import com.netflix.asgard.plugin.AdvancedUserDataProvider
import com.netflix.asgard.model.LaunchContext
import javax.xml.bind.DatatypeConverter
import com.amazonaws.services.ec2.model.Image
import org.apache.zookeeper.ZooKeeper


class MyAdvancedUserDataProvider implements AdvancedUserDataProvider {

    String getZKData() {
        zk = ZooKeeper("localhost:2181", 3000, null)
        zk.start()
        String data = new String(zk.getData("/production/docker/terra/0.0.1/env", null, null), "UTF-8")
        data
    }

    String buildUserData(LaunchContext launchContext) {
        println "Something, somewhere, more or less."
        println getZKData()

        DatatypeConverter.printBase64Binary("You failed at life.".bytes)
    }
}
