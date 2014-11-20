import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityValue
import spock.lang.*
import spock.lang.Specification
/**
 * Created by debmalya.biswas on 20/10/14.
 */
class ServiceTrackerServicesTest extends Specification {
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


    def "Test Adviser entity"() {
        when:
        def createEntry = ec.entity.makeValue("service.tracker.AdviserEntry")
                .setAll([carNo: "WB-02-M", customerName: "Rahul", customerMobile: "9830984765", driverName: "Roy",
                         driverMobile: "9840857843", beforeRoadTest: "Yes", afterRoadTest: "Yes", capNumber:"1234567"]).create()
        EntityValue created = ec.entity.makeFind("service.tracker.AdviserEntry").condition("carNo", "WB-02-M").one()

        then:
        created != null

        cleanup:
        created.delete()
    }

    def "Test Security Check"() {
        when:
        def createEntry = ec.service.sync().name("tracker.TrackerServices.createSecurityCheck")
                .parameters([carNo: "WB-01-01", kmIn: "123"]).call()
        EntityValue getValue = ec.entity.makeFind("service.tracker.SecurityCheck").condition("carNo", "WB-01-01").one()
        EntityValue statusValue = ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", "WB-01-01").one()

        then:
        getValue != null
        statusValue != null
        getValue.kmIn == "123"
        statusValue.carStatus == "Reception"

        cleanup:
        getValue.delete()
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", "WB-01-01").deleteAll()
    }

    def "Test Reception Entry"(){
        when:
        def createEntrySecurity = ec.service.sync().name("tracker.TrackerServices.createSecurityCheck")
                .parameters([carNo: "WB-01-0002", kmIn: "123"]).call()
        def createEntryReception = ec.service.sync().name("tracker.TrackerServices.createReceptionEntity")
                .parameters([carNo: "WB-01-0002", job: "PM(Free)", serviceAdviser: "Abhishek Bagchi", driverOrOwner: "Owner",
                             gift: "Yes", dropCar: "Yes", customerWaiting: "No"]).call()
        EntityValue getValue = ec.entity.makeFind("service.tracker.ReceptionEntity").condition("carNo", "WB-01-0002").one()
        EntityValue getSecurity = ec.entity.makeFind("service.tracker.SecurityCheck").condition("carNo", "WB-01-0002").one()

        then:
        getValue.job == "PM(Free)"
        getValue.serviceAdviser == "Abhishek Bagchi"
        getValue.driverOrOwner == "Owner"
        getValue.gift == "Yes"
        getValue.dropCar == "Yes"
        getValue.customerWaiting == "No"

        cleanup:
        getValue.delete()
        getSecurity.delete()
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", "WB-01-0002").deleteAll()
    }

    def "Test Service Adviser Entry"(){
        when:
        ec.entity.makeFind("service.tracker.AdviserEntry").condition("carNo", "WB-01-0003").deleteAll()
        def createEntrySecurity = ec.service.sync().name("tracker.TrackerServices.createSecurityCheck")
                .parameters([carNo: "WB-01-0003", kmIn: "123"]).call()
        def createEntryReception = ec.service.sync().name("tracker.TrackerServices.createReceptionEntity")
                .parameters([carNo: "WB-01-0003", job: "PM(Free)", serviceAdviser: "Abhishek Bagchi", driverOrOwner: "Owner",
                             gift: "Yes", dropCar: "Yes", customerWaiting: "No"]).call()
        def createEntryAdviser = ec.service.sync().name("tracker.TrackerServices.createAdviserEntry")
                .parameters([carNo:"WB-01-0003", customerName:"Deb", mobileNo:"9836545651",
                             driverName:"Raj", driverMobile:"9836545651",
                             job:"PM(Free)", beforeRoadTest:"Yes", afterRoadTest:"No", capNumber: "1234567"]).call()
        EntityValue getValue = ec.entity.makeFind("service.tracker.ReceptionEntity").condition("carNo", "WB-01-0003").one()
        EntityValue getSecurity = ec.entity.makeFind("service.tracker.SecurityCheck").condition("carNo", "WB-01-0003").one()
        EntityValue getAdviser = ec.entity.makeFind("service.tracker.AdviserEntry").condition("carNo", "WB-01-0003").one()

        then:
        getValue != null
        getSecurity != null
        getAdviser != null
        getAdviser.customerName == "Deb"
        getAdviser.mobileNo == "9836545651"
        getAdviser.job == "PM(Free)"
        getAdviser.beforeRoadTest == "Yes"
        getAdviser.afterRoadTest == "No"

        cleanup:
        getSecurity.delete()
        getValue.delete()
        getAdviser.delete()
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", "WB-01-0003").deleteAll()
    }

    def "Creation of Job Controller"(){
        when:
        def createEntrySecurity = ec.service.sync().name("tracker.TrackerServices.createSecurityCheck")
                .parameters([carNo: "WB-01-00003", kmIn: "123"]).call()
        def createEntryReception = ec.service.sync().name("tracker.TrackerServices.createReceptionEntity")
                .parameters([carNo: "WB-01-00003", job: "PM(Free)", serviceAdviser: "Abhishek Bagchi", driverOrOwner: "Owner",
                             gift: "Yes", dropCar: "Yes", customerWaiting: "No"]).call()
        def createEntryAdviser = ec.service.sync().name("tracker.TrackerServices.createAdviserEntry")
                .parameters([carNo:"WB-01-00003", customerName:"Deb", mobileNo:"9836545651",
                             driverName:"Raj", driverMobile:"9836545651",
                             job:"PM(Free)", beforeRoadTest:"Yes", afterRoadTest:"No", capNumber: "123456"]).call()
        def createJobController = ec.service.sync().name("tracker.TrackerServices.createJobController")
                .parameters([carNo:"WB-01-00003" , area:"Ground Floor", bayNo:"1", technicianId:"123456", awaitingTechnician:"Yes",
                             jobId:'job1']).call()
        EntityValue getJobControllerWhenErrorOccured = ec.entity.makeFind("service.tracker.JobController").condition("carNo", "WB-01-00003").one()
        def createJobController2 = ec.service.sync().name("tracker.TrackerServices.createJobController")
                .parameters([carNo:"WB-01-00003" , area:"Ground Floor", bayNo:"234", technicianId:"123456", awaitingTechnician:"Yes",
                             jobId:'job1']).call()
        EntityValue getSecurity = ec.entity.makeFind("service.tracker.SecurityCheck").condition("carNo", "WB-01-00003").one()
        EntityValue getReception = ec.entity.makeFind("service.tracker.ReceptionEntity").condition("carNo", "WB-01-00003").one()
        EntityValue getAdviser = ec.entity.makeFind("service.tracker.AdviserEntry").condition("carNo", "WB-01-00003").one()
        EntityValue getJobController = ec.entity.makeFind("service.tracker.JobController").condition("carNo", "WB-01-00003").one()
        EntityValue getTechnician = ec.entity.makeFind("service.tracker.Technicians").condition("carNo", "WB-01-00003").one()

        then:
        getSecurity.kmIn == "123"
        getReception.serviceAdviser == "Abhishek Bagchi"
        getAdviser.customerName == "Deb"
        getJobControllerWhenErrorOccured == null
        getJobController != null
        getJobController.awaitingTechnician == "Yes"
        getTechnician != null
        getTechnician.technicianId == "123456"

        cleanup:
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", "WB-01-00003").deleteAll()
        ec.entity.makeFind("service.tracker.SecurityCheck").condition("carNo", "WB-01-00003").deleteAll()
        ec.entity.makeFind("service.tracker.ReceptionEntity").condition("carNo", "WB-01-00003").deleteAll()
        ec.entity.makeFind("service.tracker.AdviserEntry").condition("carNo", "WB-01-00003").deleteAll()
        ec.entity.makeFind("service.tracker.JobController").condition("carNo", "WB-01-00003").deleteAll()
        ec.entity.makeFind("service.tracker.Technicians").condition("carNo", "WB-01-00003").deleteAll()
    }

}