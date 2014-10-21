import org.moqui.context.ExecutionContext
import spock.lang.*
import spock.lang.Specification
import org.moqui.Moqui
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
        ec.user.loginUser("john.doe", 'moqui', null)
        ec.artifactExecution.disableAuthz()
        ec.transaction.begin(null)
    }

    def cleanup() {
        ec.transaction.commit()
        ec.artifactExecution.enableAuthz()
        ec.user.logoutUser()
    }

    def "Test for Core ArtifactGroup of Service tracker"() {

        when:
        def entityValue = ec.entity.makeFind("moqui.security.ArtifactGroup").condition("artifactGroupId", "SERVICE_TRACKER_APP").one()
        then:
        entityValue.description == "Service tracker App (via root screen)"
    }

    def "Test for ArtifactGroupMembers of Service tracker"() {

        when:
        def entityValue = ec.entity.makeFind("moqui.security.ArtifactGroupMember").condition("artifactGroupId", "SERVICE_TRACKER_APP").one()
        then:
        entityValue.artifactName == "component://ServiceTracker/screen/ServiceTracker.xml"
        entityValue.artifactTypeEnumId == "AT_XML_SCREEN"
        entityValue.inheritAuthz == "Y"
    }

   /* def "Test admin permission for service tracker"() {
        when:
        def entityValue = ec.entity.makeFind("moqui.security.ArtifactAuthz").condition("userGroupId", "ADMIN").one()
        then:
        entityValue.artifactGroupId == "SERVICE_TRACKER_APP"
        entityValue.authzTypeEnumId == "AUTHZT_ALWAYS"
        entityValue.authzActionEnumId == "AUTHZA_ALL"
    } */


}