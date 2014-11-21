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
        ec.entity.makeValue("service.tracker.AdviserEntry")
                .setAll([carNo: "WB-02-0001", customerName: "Rahul", customerMobile: "9830984765", driverName: "Roy",
                         driverMobile: "9840857843", beforeRoadTest: "Yes", afterRoadTest: "Yes", capNumber:"1234567"]).create()
        EntityValue created = ec.entity.makeFind("service.tracker.AdviserEntry").condition("carNo", "WB-02-0001").one()

        then:
        created != null

        cleanup:
        created.delete()
    }

    def "Test Security Check"() {
        when:
        ec.service.sync().name("tracker.TrackerServices.createSecurityCheck")
                .parameters([carNo: "WB-02-0002", kmIn: "123"]).call()
        EntityValue getValue = ec.entity.makeFind("service.tracker.SecurityCheck").condition("carNo", "WB-02-0002").one()
        EntityValue statusValue = ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", "WB-02-0002").one()

        then:
        getValue != null
        statusValue != null
        getValue.kmIn == "123"
        statusValue.carStatus == "Reception"

        cleanup:
        getValue.delete()
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", "WB-02-0002").deleteAll()
    }

    def "Test Reception Entry"(){
        when:
        ec.service.sync().name("tracker.TrackerServices.createSecurityCheck")
                .parameters([carNo: "WB-02-0003", kmIn: "123"]).call()
        ec.service.sync().name("tracker.TrackerServices.createReceptionEntity")
                .parameters([carNo: "WB-02-0003", job: "PM(Free)", serviceAdviser: "Abhishek Bagchi", driverOrOwner: "Owner",
                             gift: "Yes", dropCar: "Yes", customerWaiting: "No"]).call()
        EntityValue getValue = ec.entity.makeFind("service.tracker.ReceptionEntity").condition("carNo", "WB-02-0003").one()
        EntityValue getSecurity = ec.entity.makeFind("service.tracker.SecurityCheck").condition("carNo", "WB-02-0003").one()

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
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", "WB-02-0003").deleteAll()
    }

    def "Test Service Adviser Entry"(){
        when:
        ec.entity.makeFind("service.tracker.AdviserEntry").condition("carNo", "WB-02-0004").deleteAll()
        ec.service.sync().name("tracker.TrackerServices.createSecurityCheck")
                .parameters([carNo: "WB-02-0004", kmIn: "123"]).call()
        ec.service.sync().name("tracker.TrackerServices.createReceptionEntity")
                .parameters([carNo: "WB-02-0004", job: "PM(Free)", serviceAdviser: "Abhishek Bagchi", driverOrOwner: "Owner",
                             gift: "Yes", dropCar: "Yes", customerWaiting: "No"]).call()
        ec.service.sync().name("tracker.TrackerServices.createAdviserEntry")
                .parameters([carNo:"WB-02-0004", customerName:"Deb", mobileNo:"9836545651",
                             driverName:"Raj", driverMobile:"9836545651",
                             job:"PM(Free)", beforeRoadTest:"Yes", afterRoadTest:"No", capNumber: "1234567"]).call()
        EntityValue getValue = ec.entity.makeFind("service.tracker.ReceptionEntity").condition("carNo", "WB-02-0004").one()
        EntityValue getSecurity = ec.entity.makeFind("service.tracker.SecurityCheck").condition("carNo", "WB-02-0004").one()
        EntityValue getAdviser = ec.entity.makeFind("service.tracker.AdviserEntry").condition("carNo", "WB-02-0004").one()

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
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", "WB-02-0004").deleteAll()
    }

    def "Creation of Job Controller"(){
        when:
        ec.service.sync().name("tracker.TrackerServices.createSecurityCheck")
                .parameters([carNo: "WB-02-0005", kmIn: "123"]).call()
        ec.service.sync().name("tracker.TrackerServices.createReceptionEntity")
                .parameters([carNo: "WB-02-0005", job: "PM(Free)", serviceAdviser: "Abhishek Bagchi", driverOrOwner: "Owner",
                             gift: "Yes", dropCar: "Yes", customerWaiting: "No"]).call()
        ec.service.sync().name("tracker.TrackerServices.createAdviserEntry")
                .parameters([carNo:"WB-02-0005", customerName:"Deb", mobileNo:"9836545651",
                             driverName:"Raj", driverMobile:"9836545651",
                             job:"PM(Free)", beforeRoadTest:"Yes", afterRoadTest:"No", capNumber: "123456"]).call()
        ec.service.sync().name("tracker.TrackerServices.createJobController")
                .parameters([carNo:"WB-02-0005" , area:"Ground Floor", bayNo:"1", technicianId:"123456", awaitingTechnician:"Yes",
                             jobId:'job1']).call()
        EntityValue getJobControllerWhenErrorOccurred = ec.entity.makeFind("service.tracker.JobController").condition("carNo", "WB-02-0005").one()
        ec.service.sync().name("tracker.TrackerServices.createJobController")
                .parameters([carNo:"WB-02-0005" , area:"Ground Floor", bayNo:"234", technicianId:"123456", awaitingTechnician:"Yes",
                             jobId:'job1']).call()
        EntityValue getSecurity = ec.entity.makeFind("service.tracker.SecurityCheck").condition("carNo", "WB-02-0005").one()
        EntityValue getReception = ec.entity.makeFind("service.tracker.ReceptionEntity").condition("carNo", "WB-02-0005").one()
        EntityValue getAdviser = ec.entity.makeFind("service.tracker.AdviserEntry").condition("carNo", "WB-02-0005").one()
        EntityValue getJobController = ec.entity.makeFind("service.tracker.JobController").condition("carNo", "WB-02-0005").one()
        EntityValue getTechnician = ec.entity.makeFind("service.tracker.Technicians").condition("carNo", "WB-02-0005").one()

        then:
        getSecurity.kmIn == "123"
        getReception.serviceAdviser == "Abhishek Bagchi"
        getAdviser.customerName == "Deb"
        getJobControllerWhenErrorOccurred == null
        getJobController != null
        getJobController.awaitingTechnician == "Yes"
        getTechnician != null
        getTechnician.technicianId == "123456"

        cleanup:
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", "WB-02-0005").deleteAll()
        ec.entity.makeFind("service.tracker.SecurityCheck").condition("carNo", "WB-02-0005").deleteAll()
        ec.entity.makeFind("service.tracker.ReceptionEntity").condition("carNo", "WB-02-0005").deleteAll()
        ec.entity.makeFind("service.tracker.AdviserEntry").condition("carNo", "WB-02-0005").deleteAll()
        ec.entity.makeFind("service.tracker.JobController").condition("carNo", "WB-02-0005").deleteAll()
        ec.entity.makeFind("service.tracker.Technicians").condition("carNo", "WB-02-0005").deleteAll()
    }

    def "Remove car from washing section which is ready for delivered"() {
        when:
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", "WB-02-0006").deleteAll()
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:"WB-02-0006", carStatus:"Washing", inTime:ec.user.getNowTimestamp()]).create()
        ec.service.sync().name("tracker.TrackerServices.doneWashingStatusOfCar").parameters([carNo:"WB-02-0006"]).call()
        def nullWashingCar = ec.entity.makeFind("service.tracker.StatusOfCar").condition([carNo:"WB-02-0006", outTime:null, carStatus:'Washing']).list()
        def getWashingCar = ec.entity.makeFind("service.tracker.StatusOfCar").condition([carNo:"WB-02-0006"]).one()

        then:
        nullWashingCar.isEmpty()
        getWashingCar.inTime != null
        getWashingCar.carStatus == 'Ready for Delivered'

        cleanup:
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", "WB-02-0006").deleteAll()
    }

    def "Check all the car list in reception section"() {
        when:
        ec.service.sync().name("tracker.TrackerServices.createSecurityCheck")
                .parameters([carNo: "WB-02-0007", kmIn: "123"]).call()
        Map getReceptionList = ec.service.sync().name("tracker.TrackerServices.getSecurityAndCarStatus").call()

        then:
        getReceptionList != null
        getReceptionList.receptionCarList.kmIn == ["123"]
        getReceptionList.receptionCarList.carNo == ["WB-02-0007"]
        getReceptionList.receptionCarList.outTime == [null]
        getReceptionList.receptionCarList.carStatus == ["Reception"]

        cleanup:
        ec.entity.makeFind("service.tracker.SecurityCheck").condition("carNo", "WB-02-0007").deleteAll()
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", "WB-02-0007").deleteAll()
    }

    def "Check all the car list in Adviser section"() {
        when:
        ec.entity.makeValue("service.tracker.ReceptionEntity").setAll([carNo:"WB-02-0009", job:"PM(Free)", serviceAdviser:"Abhishek Bagchi", driverOrOwner:"Owner",
                                                                       gift:"Yes", dropCar:"Yes", customerWaiting:"No"]).create()
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:"WB-02-0009", carStatus: "Service Adviser"]).create()
        def getAdviserList = ec.service.sync().name("tracker.TrackerServices.getReceptionEntityAndCarStatus").call()

        then:
        getAdviserList != null
        getAdviserList.adviserCarList.carNo != null
        getAdviserList.adviserCarList.job != null
        getAdviserList.adviserCarList.serviceAdviser != null
        getAdviserList.adviserCarList.driverOrOwner != null
        getAdviserList.adviserCarList.gift != null
        getAdviserList.adviserCarList.dropCar != null
        getAdviserList.adviserCarList.customerWaiting != null
        getAdviserList.adviserCarList.carStatus != null

        cleanup:
        ec.entity.makeFind("service.tracker.ReceptionEntity").condition("carNo", "WB-02-0009").deleteAll()
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", "WB-02-0009").deleteAll()
    }

    def "Check all the car list in Job Controller section"() {
        when:
        ec.entity.makeFind("service.tracker.AdviserEntry").condition("carNo", "WB-02-0010").deleteAll()
        ec.entity.makeValue("service.tracker.AdviserEntry").setAll([carNo:"WB-02-0010", customerName:"Deb", mobileNo:"9836545651",
                                                                    driverName:"Raj", driverMobile:"9836545651",
                                                                    job:"PM(Free)", beforeRoadTest:"Yes", afterRoadTest:"No", capNumber: "123456"]).create()
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:"WB-02-0010", carStatus: "Job Controller"]).create()
        Map getControllerList = ec.service.sync().name("tracker.TrackerServices.getAdviserEntryAndCarStatus").call()

        then:
        getControllerList != null
        getControllerList.jobControllerCarList.carNo != null
        getControllerList.jobControllerCarList.customerName != null
        getControllerList.jobControllerCarList.mobileNo != null
        getControllerList.jobControllerCarList.driverName != null
        getControllerList.jobControllerCarList.driverMobile != null
        getControllerList.jobControllerCarList.job != null
        getControllerList.jobControllerCarList.beforeRoadTest != null
        getControllerList.jobControllerCarList.afterRoadTest != null
        getControllerList.jobControllerCarList.carStatus != null

        cleanup:
        ec.entity.makeFind("service.tracker.AdviserEntry").condition("carNo", "WB-02-0010").deleteAll()
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", "WB-02-0010").deleteAll()
    }

    def "Check all the car list in washing section"() {
        when:
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:"WB-02-0011", carStatus: "Job Controller"]).create()
        def washingCarList = ec.service.sync().name("tracker.TrackerServices.getWashingStatusOfCar").call()

        then:
        washingCarList != null
        washingCarList.washingList.carNo != null
        washingCarList.washingList.carStatus != null

        cleanup:
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", "WB-02-0011").deleteAll()
    }

    def "Check all the car list in Get delivery section"() {
        when:
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:"WB-02-0012", carStatus: "Ready for Delivered"]).create()
        def deliveryCarList = ec.service.sync().name("tracker.TrackerServices.getDeliveryStatusOfCar").call()

        then:
        deliveryCarList != null
        deliveryCarList.deliveryList.carNo != null
        deliveryCarList.deliveryList.carStatus != null

        cleanup:
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", "WB-02-0012").deleteAll()
    }

    def "Check all the car list in Technician section"() {
        when:
        ec.entity.makeValue("service.tracker.JobController").setAll([carNo:"WB-02-0013" , area:"Ground Floor", bayNo:"1", technicianId:"123456", awaitingTechnician:"Yes",
                                                                     jobId:'job1', fromDate:ec.user.getNowTimestamp()]).create()
        ec.entity.makeValue("service.tracker.Technicians").setAll([carNo:"WB-02-0013", jobId:'job1', technicianId:"123456", fromDate:ec.user.getNowTimestamp()]).create()
        def technicianList = ec.service.sync().name("tracker.TrackerServices.getDeliveryStatusOfCar").call()

        then:
        technicianList != null

    }

}