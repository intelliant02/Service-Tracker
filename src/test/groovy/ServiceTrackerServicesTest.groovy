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
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:"WB-02-0003", carStatus: "Reception"]).create()
        ec.service.sync().name("tracker.TrackerServices.createReceptionEntity")
                .parameters([carNo: "WB-02-0003", job: "PM(Free)", serviceAdviser: "Abhishek Bagchi(101)", driverOrOwner: "Owner",
                             gift: "Yes", dropCar: "Yes", customerWaiting: "No"]).call()
        EntityValue getValue = ec.entity.makeFind("service.tracker.ReceptionEntity").condition("carNo", "WB-02-0003").one()

        then:
        getValue.job == "PM(Free)"
        getValue.serviceAdviser == "Abhishek Bagchi(101)"
        getValue.driverOrOwner == "Owner"
        getValue.gift == "Yes"
        getValue.dropCar == "Yes"
        getValue.customerWaiting == "No"

        cleanup:
        ec.entity.makeFind("service.tracker.ReceptionEntity").condition("carNo", "WB-02-0003").deleteAll()
        ec.entity.makeFind("service.tracker.SecurityCheck").condition("carNo", "WB-02-0003").deleteAll()
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", "WB-02-0003").deleteAll()
    }

    def "Check all the car list in reception section"() {
        when:
        ec.service.sync().name("tracker.TrackerServices.createSecurityCheck")
                .parameters([carNo: "WB-02-0007", kmIn: "123"]).call()
        Map getReceptionList = ec.service.sync().name("tracker.TrackerServices.getSecurityAndCarStatus").call()

        then:
        (getReceptionList.receptionCarList.carNo).contains("WB-02-0007")
        (getReceptionList.receptionCarList.kmIn).contains("123")

        cleanup:
        ec.entity.makeFind("service.tracker.SecurityCheck").condition("carNo", "WB-02-0007").deleteAll()
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", "WB-02-0007").deleteAll()
    }

    def "Check all the car list in Adviser section"() {
        when:
        ec.entity.makeValue("service.tracker.ReceptionEntity").setAll([carNo:"WB-02G-00007", job:"PM(Free)", serviceAdviser:"Abhishek Bagchi(101)", driverOrOwner:"Owner",
                                                                       gift:"Yes", dropCar:"Yes", customerWaiting:"No"]).create()
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:"WB-02G-00007", carStatus: "Service Adviser"]).create()
        def getAdviserList = ec.service.sync().name("tracker.TrackerServices.getReceptionEntityAndCarStatus").call()

        then:
        (getAdviserList.adviserCarList.carNo).contains("WB-02G-00007")
        (getAdviserList.adviserCarList.serviceAdviser).contains("Abhishek Bagchi(101)")
        (getAdviserList.adviserCarList.carStatus).contains("Service Adviser")

        cleanup:
        ec.entity.makeFind("service.tracker.ReceptionEntity").condition("carNo", "WB-02G-00007").deleteAll()
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", "WB-02G-00007").deleteAll()
    }

    def "Remove car from washing section which is ready for delivered"() {
        when:
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

    def "Creation of Job Controller"(){
        when:
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:"WB-02G-00007", carStatus:"Job Controller"]).create()
        ec.service.sync().name("tracker.TrackerServices.createJobController")
                .parameters([carNo:"WB-02G-00007" , area:"Ground floor", bayNo:"2", technicianId:"123456", awaitingTechnician:"Yes",
                             jobId:'job1']).call()
        EntityValue getJobController = ec.entity.makeFind("service.tracker.JobController").condition("carNo", "WB-02G-00007").one()
        EntityValue getTechnician = ec.entity.makeFind("service.tracker.Technicians").condition("carNo", "WB-02G-00007").one()

        then:
        getJobController != null
        getJobController.awaitingTechnician == "Yes"
        getTechnician != null
        getTechnician.technicianId == "123456"

        cleanup:
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", "WB-02G-00007").deleteAll()
        ec.entity.makeFind("service.tracker.JobController").condition("carNo", "WB-02G-00007").deleteAll()
        ec.entity.makeFind("service.tracker.Technicians").condition("carNo", "WB-02G-00007").deleteAll()
        def updateCarPlace = ec.entity.makeFind("service.internal.CarPlaces").condition("placeValue", "Ground floor 2").one()
        updateCarPlace.vacant = '0'
        updateCarPlace.update()
    }

    def "Test Service Adviser Entry"(){
        when:
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:"WB-02-0004", carStatus:"Service Adviser"]).create()
        ec.service.sync().name("tracker.TrackerServices.createAdviserEntry")
                .parameters([carNo:"WB-02-0004", customerName:"Deb", mobileNo:"9836545651",
                             driverName:"Raj", driverMobile:"9836545651",
                             job:"PM(Free)", beforeRoadTest:"Yes", afterRoadTest:"No",
                             capNumber: "1234567", promisedTime:"2014-11-27 11:55"]).call()
        EntityValue getAdviser = ec.entity.makeFind("service.tracker.AdviserEntry").condition("carNo", "WB-02-0004").one()

        then:
        getAdviser != null
        getAdviser.customerName == "Deb"
        getAdviser.mobileNo == "9836545651"
        getAdviser.job == "PM(Free)"
        getAdviser.beforeRoadTest == "Yes"
        getAdviser.afterRoadTest == "No"
        getAdviser.promisedTime != null

        cleanup:
        ec.entity.makeFind("service.tracker.AdviserEntry").condition("carNo", "WB-02-0004").deleteAll()
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", "WB-02-0004").deleteAll()
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
        getControllerList.jobControllerCarList.carNo.contains("WB-02-0010")
        getControllerList.jobControllerCarList.customerName.contains("Deb")
        getControllerList.jobControllerCarList.mobileNo.contains("9836545651")
        getControllerList.jobControllerCarList.driverName.contains("Raj")
        getControllerList.jobControllerCarList.driverMobile.contains("9836545651")
        getControllerList.jobControllerCarList.job.contains("PM(Free)")
        getControllerList.jobControllerCarList.beforeRoadTest.contains("Yes")
        getControllerList.jobControllerCarList.afterRoadTest.contains("No")
        getControllerList.jobControllerCarList.carStatus.contains("Job Controller")

        cleanup:
        ec.entity.makeFind("service.tracker.AdviserEntry").condition("carNo", "WB-02-0010").deleteAll()
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", "WB-02-0010").deleteAll()
    }

    def "Check all the car list in washing section"() {
        when:
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:"WB-02-0011", carStatus: "Washing"]).create()
        def washingCarList = ec.service.sync().name("tracker.TrackerServices.getWashingStatusOfCar").call()

        then:
        washingCarList != null
        (washingCarList.washingList.carNo).contains("WB-02-0011")
        (washingCarList.washingList.carStatus).contains("Washing")

        cleanup:
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", "WB-02-0011").deleteAll()
    }

    def "Check all the car list in Get delivery section"() {
        when:
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:"WB-02-0012", carStatus: "Ready for Delivered"]).create()
        def deliveryCarList = ec.service.sync().name("tracker.TrackerServices.getDeliveryStatusOfCar").call()

        then:
        (deliveryCarList.deliveryList.carNo).contains("WB-02-0012")
        (deliveryCarList.deliveryList.carStatus).contains("Ready for Delivered")

        cleanup:
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", "WB-02-0012").deleteAll()
    }

    def "Check all the car list in Technician section"() {
        when:
        ec.entity.makeValue("service.tracker.JobController").setAll([carNo:"WB-02-0013" , area:"Ground floor", bayNo:"1", awaitingTechnician:"Yes",
                                                                     fromDate:ec.user.getNowTimestamp()]).create()
        ec.entity.makeValue("service.tracker.Technicians").setAll([carNo:"WB-02-0013", jobId:'Job1', technicianId:"123456", fromDate:ec.user.getNowTimestamp()]).create()

        ec.entity.makeValue("service.tracker.JobController").setAll([carNo:"WB-02-0014" , area:"First floor", bayNo:"1", awaitingTechnician:"Yes",
                                                                     fromDate:ec.user.getNowTimestamp()]).create()
        ec.entity.makeValue("service.tracker.Technicians").setAll([carNo:"WB-02-0014", jobId:'Job2', technicianId:"12345", fromDate:ec.user.getNowTimestamp()]).create()
        def getTechnicianList = ec.service.sync().name("tracker.TrackerServices.getJobControllerAndTechnician").call()
        def getFirstFloorTechnicianList = ec.service.sync().name("tracker.TrackerServices.getJobControllerAndTechnician").call()
        then:
        (getTechnicianList.technicianList.carNo).contains("WB-02-0013")
        (getTechnicianList.technicianList.area).contains("Ground floor")
        (getTechnicianList.technicianList.bayNo).contains("1")
        (getTechnicianList.technicianList.technicianId).contains("123456")
        (getTechnicianList.technicianList.awaitingTechnician).contains("Yes")
        (getTechnicianList.technicianList.jobId).contains("Job1")
        (getFirstFloorTechnicianList.technicianListFirstFloor.carNo).contains("WB-02-0014")
        (getFirstFloorTechnicianList.technicianListFirstFloor.area).contains("First floor")
        (getFirstFloorTechnicianList.technicianListFirstFloor.bayNo).contains("1")
        (getFirstFloorTechnicianList.technicianListFirstFloor.technicianId).contains("12345")
        (getFirstFloorTechnicianList.technicianListFirstFloor.awaitingTechnician).contains("Yes")
        (getFirstFloorTechnicianList.technicianListFirstFloor.jobId).contains("Job2")

        cleanup:
        ec.entity.makeFind("service.tracker.JobController").condition("carNo", "WB-02-0013").deleteAll()
        ec.entity.makeFind("service.tracker.Technicians").condition("carNo", "WB-02-0013").deleteAll()
        ec.entity.makeFind("service.tracker.JobController").condition("carNo", "WB-02-0014").deleteAll()
        ec.entity.makeFind("service.tracker.Technicians").condition("carNo", "WB-02-0014").deleteAll()
    }

    def "Check all the car list in Delivery section"() {
        when:
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:"WB-02-0013", carStatus:"Ready for Delivered"]).create()
        def getDeliveryList = ec.service.sync().name("tracker.TrackerServices.getDeliveryStatusOfCar").call()

        then:
        (getDeliveryList.deliveryList.carNo).contains("WB-02-0013")
        (getDeliveryList.deliveryList.carStatus).contains("Ready for Delivered")

        cleanup:
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", "WB-02-0013").deleteAll()
    }

    def "Test for test drive of technician"() {
        when:
        ec.service.sync().name("tracker.TrackerServices.createTestDriveTechnician").parameters([carNo: "WB-02G-00006", kmIn:"123455"]).call()
        ec.service.sync().name("tracker.TrackerServices.createTestDriveTechnician").parameters([carNo:"WB-02G-00006", kmOut:"123456"]).call()
        EntityValue getTotalTestDrive = ec.entity.makeFind("service.tracker.TestDriveTechnician").condition("carNo", "WB-02G-00006").one()
        then:
        getTotalTestDrive != null
        getTotalTestDrive.testKey != null
        getTotalTestDrive.kmIn == 123455
        getTotalTestDrive.kmOut == 123456

        cleanup:
        ec.entity.makeFind("service.tracker.TestDriveTechnician").condition("carNo", "WB-02G-00006").deleteAll()
    }

    def "Test for Car Delivered to Customer"() {
        when:
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:"WB-02-0013", carStatus:"Ready for Delivered"]).create()
        ec.service.sync().name("tracker.TrackerServices.carDeliveredStatusOfCar").parameters([carNo:"WB-02-0013"]).call()
        EntityValue getCarDelivered = ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", "WB-02-0013").one()

        then:
        getCarDelivered.outTime != null

        cleanup:
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", "WB-02-0013").deleteAll()
    }

}
