import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import spock.lang.Shared
import spock.lang.Specification
import testdata.UserData

/**
 * Created by debmalya.biswas on 21/10/14.
 */
class SeedDataTest extends Specification {
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

    def "Test for Car status type"() {
        when:
        def entityList = ec.entity.makeFind("service.tracker.CarStatusType").list()
        then:
        entityList.size() == 8
    }

    def "Test for Service adviser"() {
        when:
        def entityList = ec.entity.makeFind("service.internal.ServiceAdviser").list()
        then:
        entityList.size() == 6
    }

    def "Test for Customer Details"() {
        when:
        def entityList = ec.entity.makeFind("service.internal.CustomerDetails").list()
        then:
        entityList.size() == 5
    }

    def "Test for Car places"() {
        when:
        def entityList = ec.entity.makeFind("service.internal.CarPlaces").list()
        then:
        entityList.size() == 6
    }

    def "Test for Vacant field"() {
        when:
        def showResult = ec.entity.makeFind("service.internal.CarPlaces").condition("placeValue", "Ground floor 3").one()
        then:
        showResult.vacant == '0'
    }

    def "Test for Technician member"() {
        when:
        def showResult = ec.entity.makeFind("service.internal.TechnicianMember").list()
        then:
        showResult.size() == 8
    }
}