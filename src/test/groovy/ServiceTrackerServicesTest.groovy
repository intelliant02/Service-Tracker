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
                .setAll([CarNo: "WB-02-M", CustomerName: "Rahul", CustomerMobile: "9830984765", DriverName: "Roy",
                DriverMobile: "9840857843", BeforeRoadTest: "Yes", AfterRoadTest: "Yes"]).create()
        EntityValue created = ec.entity.makeFind("service.tracker.AdviserEntry").condition("CarNo", "WB-02-M").one()
        then:
        created != null
        cleanup:
        created.delete()
    }

    def "Test Security Check"() {
        when:
        def createEntry = ec.service.sync().name("tracker.TrackerServices.createSecurityCheck")
                .parameters([CarNo: "WB-01-01", KmIn: "123"]).call()
        EntityValue getValue = ec.entity.makeFind("service.tracker.SecurityCheck").condition("CarNo", "WB-01-01").one()
        EntityValue statusValue = ec.entity.makeFind("service.tracker.StatusOfCar").condition("CarNo", "WB-01-01").one()
        then:
        getValue != null
        statusValue != null
        getValue.KmIn == "123"
        statusValue.CarStatus == "Reception"
        cleanup:
        getValue.delete()
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("CarNo", "WB-01-01").deleteAll()
    }

    def "Test Reception Entry"(){
        when:
        def createEntrySecurity = ec.service.sync().name("tracker.TrackerServices.createSecurityCheck")
                .parameters([CarNo: "WB-01-0002", KmIn: "123"]).call()
        def createEntryReception = ec.service.sync().name("tracker.TrackerServices.createReceptionEntity")
                .parameters([CarNo: "WB-01-0002", Job: "PM(Free)", ServiceAdviser: "Abhishek Bagchi", DriverOrOwner: "Owner",
                Gift: "Yes", DropCar: "Yes", CustomerWaiting: "No"]).call()
        EntityValue getValue = ec.entity.makeFind("service.tracker.ReceptionEntity").condition("CarNo", "WB-01-0002").one()
        EntityValue getStatus = ec.entity.makeFind("service.tracker.StatusOfCar").condition("CarNo", "WB-01-0002").one()
        EntityValue getSecurity = ec.entity.makeFind("service.tracker.SecurityCheck").condition("CarNo", "WB-01-0002").one()
        then:
        getValue.Job == "PM(Free)"
        getValue.ServiceAdviser == "Abhishek Bagchi"
        getValue.DriverOrOwner == "Owner"
        getValue.Gift == "Yes"
        getValue.DropCar == "Yes"
        getValue.CustomerWaiting == "No"
        cleanup:
        getValue.delete()
        getSecurity.delete()
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("CarNo", "WB-01-0002").deleteAll()
    }

    def "Test Service Adviser Entry"(){
        when:
        ec.entity.makeFind("service.tracker.AdviserEntry").condition("CarNo", "WB-01-0003").deleteAll()
        def createEntrySecurity = ec.service.sync().name("tracker.TrackerServices.createSecurityCheck")
                .parameters([CarNo: "WB-01-0003", KmIn: "123"]).call()
        def createEntryReception = ec.service.sync().name("tracker.TrackerServices.createReceptionEntity")
                .parameters([CarNo: "WB-01-0003", Job: "PM(Free)", ServiceAdviser: "Abhishek Bagchi", DriverOrOwner: "Owner",
                Gift: "Yes", DropCar: "Yes", CustomerWaiting: "No"]).call()
        def createEntryAdviser = ec.service.sync().name("tracker.TrackerServices.createAdviserEntry")
                .parameters([CarNo:"WB-01-0003", CustomerName:"Deb", MobileNo:"9836545651",
                DriverName:"Raj", DriverMobile:"9836545651",
                Job:"PM(Free)", BeforeRoadTest:"Yes", AfterRoadTest:"No"]).call()
        EntityValue getValue = ec.entity.makeFind("service.tracker.ReceptionEntity").condition("CarNo", "WB-01-0003").one()
        EntityValue getSecurity = ec.entity.makeFind("service.tracker.SecurityCheck").condition("CarNo", "WB-01-0003").one()
        EntityValue getAdviser = ec.entity.makeFind("service.tracker.AdviserEntry").condition("CarNo", "WB-01-0003").one()

        then:
        getValue != null
        getSecurity != null
        getAdviser != null
        getAdviser.CustomerName == "Deb"
        getAdviser.MobileNo == "9836545651"
        getAdviser.Job == "PM(Free)"
        getAdviser.BeforeRoadTest == "Yes"
        getAdviser.AfterRoadTest == "No"
        cleanup:
        getSecurity.delete()
        getValue.delete()
        getAdviser.delete()
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("CarNo", "WB-01-0003").deleteAll()

    }
    def "Creation of Job Controller"(){
        when:
        def createEntrySecurity = ec.service.sync().name("tracker.TrackerServices.createSecurityCheck")
                .parameters([CarNo: "WB-01-00003", KmIn: "123"]).call()
        def createEntryReception = ec.service.sync().name("tracker.TrackerServices.createReceptionEntity")
                .parameters([CarNo: "WB-01-00003", Job: "PM(Free)", ServiceAdviser: "Abhishek Bagchi", DriverOrOwner: "Owner",
                Gift: "Yes", DropCar: "Yes", CustomerWaiting: "No"]).call()
        def createEntryAdviser = ec.service.sync().name("tracker.TrackerServices.createAdviserEntry")
                .parameters([CarNo:"WB-01-00003", CustomerName:"Deb", MobileNo:"9836545651",
                DriverName:"Raj", DriverMobile:"9836545651",
                Job:"PM(Free)", BeforeRoadTest:"Yes", AfterRoadTest:"No"]).call()
        def createJobController = ec.service.sync().name("tracker.TrackerServices.createJobController")
                .parameters([CarNo:"WB-01-00003" , Area:"Ground Floor", BayNo:"1", TechnicianId:"123456", AwaitingTechnician:"Yes",
                JobId:'job1']).call()
        EntityValue getSecurity = ec.entity.makeFind("service.tracker.SecurityCheck").condition("CarNo", "WB-01-00003").one()
        EntityValue getReception = ec.entity.makeFind("service.tracker.ReceptionEntity").condition("CarNo", "WB-01-00003").one()
        EntityValue getAdviser = ec.entity.makeFind("service.tracker.AdviserEntry").condition("CarNo", "WB-01-00003").one()
        EntityValue getJobController = ec.entity.makeFind("service.tracker.JobController").condition("CarNo", "WB-01-00003").one()
        EntityValue getTechnician = ec.entity.makeFind("service.tracker.Technicians").condition("CarNo", "WB-01-00003").one()
        then:
        getSecurity.KmIn == "123"
        getReception.ServiceAdviser == "Abhishek Bagchi"
        getAdviser.CustomerName == "Deb"
        getJobController != null
        getJobController.AwaitingTechnician == "Yes"
        getTechnician != null
        getTechnician.TechnicianId == "123456"

        cleanup:
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("CarNo", "WB-01-00003").deleteAll()
        ec.entity.makeFind("service.tracker.SecurityCheck").condition("CarNo", "WB-01-00003").deleteAll()
        ec.entity.makeFind("service.tracker.ReceptionEntity").condition("CarNo", "WB-01-00003").deleteAll()
        ec.entity.makeFind("service.tracker.AdviserEntry").condition("CarNo", "WB-01-00003").deleteAll()
        ec.entity.makeFind("service.tracker.JobController").condition("CarNo", "WB-01-00003").deleteAll()
        ec.entity.makeFind("service.tracker.Technicians").condition("CarNo", "WB-01-00003").deleteAll()
    }

}