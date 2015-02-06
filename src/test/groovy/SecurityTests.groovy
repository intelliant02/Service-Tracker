import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import spock.lang.Shared
import spock.lang.Specification
import testdata.UserData

/**
 * Created by debmalya.biswas on 20/10/14.
 */
class SecurityTests extends Specification {
    @Shared
    ExecutionContext ec

    def setupSpec() {
        ec = Moqui.getExecutionContext()
    }

    def cleanupSpec() {
        ec.destroy()
    }

    def setup() {
        ec.user.loginUser(UserData.userName, UserData.passWord, null)
        ec.artifactExecution.disableAuthz()
        ec.transaction.begin(null)
    }

    def cleanup() {
        ec.transaction.commit()
        ec.artifactExecution.enableAuthz()
        ec.user.logoutUser()
    }

    def "test for core ArtifactGroup of Service tracker"() {

        when:
        def entityValue = ec.entity.makeFind("moqui.security.ArtifactGroup").condition("artifactGroupId", "SERVICE_TRACKER_APP").one()
        then:
        entityValue.description == "Service tracker App (via root screen)"
    }

    def "test for ArtifactGroupMembers of Service tracker"() {

        when:
        def entityValue = ec.entity.makeFind("moqui.security.ArtifactGroupMember").condition("artifactGroupId", "SERVICE_TRACKER_APP").one()
        then:
        entityValue.artifactName == "component://ServiceTracker/screen/ServiceTracker.xml"
        entityValue.artifactTypeEnumId == "AT_XML_SCREEN"
        entityValue.inheritAuthz == "Y"
    }
}