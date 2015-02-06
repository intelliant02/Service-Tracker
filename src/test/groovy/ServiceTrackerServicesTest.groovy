import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityValue
import spock.lang.*
import spock.lang.Specification
import testdata.CarData
import testdata.MockTime
import testdata.StatusType
import testdata.UserData

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
        ec.user.loginUser(UserData.userName, UserData.passWord, null)
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
        ec.user.setEffectiveTime(MockTime.getTime())
        ec.service.sync().name("tracker.TrackerServices.createSecurityCheck")
                .parameters([carNo: CarData.carNo, kmIn: CarData.kmIn]).call()
        EntityValue getValue = ec.entity.makeFind("service.tracker.SecurityCheck").condition("carNo", CarData.carNo).one()
        EntityValue statusValue = ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", CarData.carNo).one()
        then:
        getValue != null
        statusValue != null
        getValue.kmIn == CarData.kmIn
        statusValue.carStatus == StatusType.inReception
        statusValue.inTime == MockTime.getTime()

        cleanup:
        ec.user.setEffectiveTime(null)
        getValue.delete()
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", CarData.carNo).deleteAll()

    }

    def "Test Reception Entry"() {
        when:
        ec.user.setEffectiveTime(MockTime.getTime())
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:CarData.carNo, carStatus:StatusType.inReception]).create()
        ec.service.sync().name("tracker.TrackerServices.createReceptionEntity")
                .parameters([carNo: CarData.carNo, job: "PM(Free)", serviceAdviser: "Abhishek Bagchi(101)", driverOrOwner: "Owner",
                             gift: "Yes", dropCar: "Yes", customerWaiting: "No"]).call()
        EntityValue getValue = ec.entity.makeFind("service.tracker.ReceptionEntity").condition("carNo", CarData.carNo).one()
        EntityValue getStatusOfCar = ec.entity.makeFind("service.tracker.StatusOfCar").condition([carNo:CarData.carNo, carStatus:StatusType.inAdviser]).one()

        then:
        getValue.job == "PM(Free)"
        getValue.serviceAdviser == "Abhishek Bagchi(101)"
        getValue.driverOrOwner == "Owner"
        getValue.gift == "Yes"
        getValue.dropCar == "Yes"
        getValue.customerWaiting == "No"
        getStatusOfCar.inTime == MockTime.getTime()

        cleanup:
        ec.user.setEffectiveTime(null)
        ec.entity.makeFind("service.tracker.ReceptionEntity").condition("carNo", CarData.carNo).deleteAll()
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", CarData.carNo).deleteAll()
    }

    def "Check all the car list in reception section"() {
        when:
        ec.service.sync().name("tracker.TrackerServices.createSecurityCheck")
                .parameters([carNo: CarData.carNo, kmIn: CarData.kmIn]).call()
        Map getReceptionList = ec.service.sync().name("tracker.TrackerServices.getSecurityAndCarStatus").call()

        then:
        (getReceptionList.receptionCarList.carNo).contains(CarData.carNo)
        (getReceptionList.receptionCarList.kmIn).last() == CarData.kmIn

        cleanup:
        ec.entity.makeFind("service.tracker.SecurityCheck").condition("carNo", CarData.carNo).deleteAll()
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", CarData.carNo).deleteAll()
    }

    def "Check all the car list in Adviser section"() {
        when:
        ec.entity.makeValue("service.tracker.ReceptionEntity").setAll([carNo:CarData.carNo, job:"PM(Free)", serviceAdviser:"Abhishek Bagchi(101)", driverOrOwner:"Owner",
                                                                       gift:"Yes", dropCar:"Yes", customerWaiting:"No"]).create()
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:CarData.carNo, carStatus:StatusType.inAdviser]).create()
        def getAdviserList = ec.service.sync().name("tracker.TrackerServices.getReceptionEntityAndCarStatus").call()

        then:
        (getAdviserList.adviserCarList.carNo).contains(CarData.carNo)
        (getAdviserList.adviserCarList.serviceAdviser).contains("Abhishek Bagchi(101)")
        (getAdviserList.adviserCarList.carStatus).contains(StatusType.inAdviser)

        cleanup:
        ec.entity.makeFind("service.tracker.ReceptionEntity").condition("carNo", CarData.carNo).deleteAll()
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", CarData.carNo).deleteAll()
    }

    def "Remove car from washing section which is ready for delivered"() {
        when:
        ec.user.setEffectiveTime(MockTime.getTime())
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:CarData.carNo, carStatus:StatusType.inWasher, inTime:ec.user.getNowTimestamp()]).create()
        ec.service.sync().name("tracker.TrackerServices.doneWashingStatusOfCar").parameters([carNo:CarData.carNo]).call()
        def nullWashingCar = ec.entity.makeFind("service.tracker.StatusOfCar").condition([carNo:CarData.carNo, outTime:null, carStatus:StatusType.inWasher]).list()
        def getWashingCar = ec.entity.makeFind("service.tracker.StatusOfCar").condition([carNo:CarData.carNo]).one()

        then:
        nullWashingCar.isEmpty()
        getWashingCar.inTime == MockTime.getTime()
        getWashingCar.carStatus == StatusType.inDeliver

        cleanup:
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", CarData.carNo).deleteAll()
        ec.user.setEffectiveTime(null)
    }

    def "Creation of Job Controller"() {
        when:
        ec.user.setEffectiveTime(MockTime.getTime())
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:CarData.carNo, carStatus:StatusType.inController]).create()
        ec.service.sync().name("tracker.TrackerServices.createJobController")
                .parameters([carNo:CarData.carNo , area:"Ground floor", bayNo:"2", technicianId:"123456", awaitingTechnician:"Yes",
                             jobId:"job1"]).call()
        EntityValue getJobController = ec.entity.makeFind("service.tracker.JobController").condition("carNo", CarData.carNo).one()
        EntityValue getOutStatus = ec.entity.makeFind("service.tracker.StatusOfCar").condition([carNo:CarData.carNo, carStatus:StatusType.inController]).one()
        EntityValue getTechnician = ec.entity.makeFind("service.tracker.Technicians").condition("carNo", CarData.carNo).one()
        EntityValue getStatus = ec.entity.makeFind("service.tracker.StatusOfCar").condition([carNo:CarData.carNo, carStatus:StatusType.inTechnician]).one()
        then:
        getJobController != null
        getJobController.awaitingTechnician == "Yes"
        getTechnician != null
        getTechnician.technicianId == "123456"
        getStatus.inTime == MockTime.getTime()
        getOutStatus.outTime == MockTime.getTime()

        cleanup:
        ec.user.setEffectiveTime(null)
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", CarData.carNo).deleteAll()
        ec.entity.makeFind("service.tracker.JobController").condition("carNo", CarData.carNo).deleteAll()
        ec.entity.makeFind("service.tracker.Technicians").condition("carNo", CarData.carNo).deleteAll()
        def updateCarPlace = ec.entity.makeFind("service.internal.CarPlaces").condition("placeValue", "Ground floor 2").one()
        updateCarPlace.vacant = "0"
        updateCarPlace.update()
    }

    def "Test Service Adviser Entry"() {
        when:
        ec.user.setEffectiveTime(MockTime.getTime())
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:CarData.carNo, carStatus:StatusType.inAdviser]).create()
        ec.service.sync().name("tracker.TrackerServices.createAdviserEntry")
                .parameters([carNo:CarData.carNo, customerName:"Deb", mobileNo:"9836545651",
                             driverName:"Raj", driverMobile:"9836545651",
                             job:"PM(Free)", beforeRoadTest:"Yes", afterRoadTest:"No",
                             capNumber: "1234567", promisedTime:"2014-11-27 11:55"]).call()
        EntityValue getAdviser = ec.entity.makeFind("service.tracker.AdviserEntry").condition("carNo", CarData.carNo).one()
        EntityValue getOutStatus = ec.entity.makeFind("service.tracker.StatusOfCar").condition([carNo:CarData.carNo, carStatus:StatusType.inAdviser]).one()
        EntityValue getInStatus = ec.entity.makeFind("service.tracker.StatusOfCar").condition([carNo:CarData.carNo, carStatus:StatusType.inController]).one()

        then:
        getAdviser != null
        getInStatus.inTime == MockTime.getTime()
        getOutStatus.outTime == MockTime.getTime()
        getAdviser.customerName == "Deb"
        getAdviser.mobileNo == "9836545651"
        getAdviser.job == "PM(Free)"
        getAdviser.beforeRoadTest == "Yes"
        getAdviser.afterRoadTest == "No"
        getAdviser.promisedTime != null

        cleanup:
        ec.user.setEffectiveTime(null)
        ec.entity.makeFind("service.tracker.AdviserEntry").condition("carNo", CarData.carNo).deleteAll()
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", CarData.carNo).deleteAll()
    }


    def "Check all the car list in Job Controller section"() {
        when:
        ec.entity.makeFind("service.tracker.AdviserEntry").condition("carNo", CarData.carNo).deleteAll()
        ec.entity.makeValue("service.tracker.AdviserEntry").setAll([carNo:CarData.carNo, customerName:"Deb", mobileNo:"9836545651",
                                                                    driverName:"Raj", driverMobile:"9836545651",
                                                                    job:"PM(Free)", beforeRoadTest:"Yes", afterRoadTest:"No", capNumber: "123456"]).create()
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:CarData.carNo, carStatus:StatusType.inController]).create()
        Map getControllerList = ec.service.sync().name("tracker.TrackerServices.getAdviserEntryAndCarStatus").call()

        then:
        getControllerList.jobControllerCarList.carNo.contains(CarData.carNo)
        getControllerList.jobControllerCarList.customerName.contains("Deb")
        getControllerList.jobControllerCarList.mobileNo.contains("9836545651")
        getControllerList.jobControllerCarList.driverName.contains("Raj")
        getControllerList.jobControllerCarList.driverMobile.contains("9836545651")
        getControllerList.jobControllerCarList.job.contains("PM(Free)")
        getControllerList.jobControllerCarList.beforeRoadTest.contains("Yes")
        getControllerList.jobControllerCarList.afterRoadTest.contains("No")
        getControllerList.jobControllerCarList.carStatus.contains(StatusType.inController)

        cleanup:
        ec.entity.makeFind("service.tracker.AdviserEntry").condition("carNo", CarData.carNo).deleteAll()
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", CarData.carNo).deleteAll()
    }

    def "Check all the car list in washing section"() {
        when:
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:CarData.carNo, carStatus:StatusType.inWasher]).create()
        def washingCarList = ec.service.sync().name("tracker.TrackerServices.getWashingStatusOfCar").call()

        then:
        washingCarList != null
        (washingCarList.washingList.carNo).contains(CarData.carNo)
        (washingCarList.washingList.carStatus).contains(StatusType.inWasher)

        cleanup:
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", CarData.carNo).deleteAll()
    }

    def "Check all the car list in Get delivery section"() {
        when:
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:CarData.carNo, carStatus:StatusType.inDeliver]).create()
        def deliveryCarList = ec.service.sync().name("tracker.TrackerServices.getDeliveryStatusOfCar").call()

        then:
        (deliveryCarList.deliveryList.carNo).contains(CarData.carNo)
        (deliveryCarList.deliveryList.carStatus).contains(StatusType.inDeliver)

        cleanup:
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", CarData.carNo).deleteAll()
    }

    def "Check all the car list in Technician section"() {
        when:
        ec.entity.makeValue("service.tracker.JobController").setAll([carNo:CarData.carNo , area:"Ground floor", bayNo:"1", awaitingTechnician:"Yes",
                                                                     fromDate:ec.user.getNowTimestamp()]).create()
        ec.entity.makeValue("service.tracker.Technicians").setAll([carNo:CarData.carNo, jobId:"Job1", technicianId:"123456", fromDate:ec.user.getNowTimestamp()]).create()

        ec.entity.makeValue("service.tracker.JobController").setAll([carNo:CarData.carNo , area:"First floor", bayNo:"1", awaitingTechnician:"Yes",
                                                                     fromDate:ec.user.getNowTimestamp()]).create()
        ec.entity.makeValue("service.tracker.Technicians").setAll([carNo:CarData.carNo, jobId:"Job2", technicianId:"12345", fromDate:ec.user.getNowTimestamp()]).create()
        def getTechnicianList = ec.service.sync().name("tracker.TrackerServices.getJobControllerAndTechnician").call()
        def getFirstFloorTechnicianList = ec.service.sync().name("tracker.TrackerServices.getJobControllerAndTechnician").call()
        then:
        (getTechnicianList.technicianList.carNo).contains(CarData.carNo)
        (getTechnicianList.technicianList.area).contains("Ground floor")
        (getTechnicianList.technicianList.bayNo).contains("1")
        (getTechnicianList.technicianList.technicianId).contains("123456")
        (getTechnicianList.technicianList.awaitingTechnician).contains("Yes")
        (getTechnicianList.technicianList.jobId).contains("Job1")
        (getFirstFloorTechnicianList.technicianListFirstFloor.carNo).contains(CarData.carNo)
        (getFirstFloorTechnicianList.technicianListFirstFloor.area).contains("First floor")
        (getFirstFloorTechnicianList.technicianListFirstFloor.bayNo).contains("1")
        (getFirstFloorTechnicianList.technicianListFirstFloor.technicianId).contains("12345")
        (getFirstFloorTechnicianList.technicianListFirstFloor.awaitingTechnician).contains("Yes")
        (getFirstFloorTechnicianList.technicianListFirstFloor.jobId).contains("Job2")

        cleanup:
        ec.entity.makeFind("service.tracker.JobController").condition("carNo", CarData.carNo).deleteAll()
        ec.entity.makeFind("service.tracker.Technicians").condition("carNo", CarData.carNo).deleteAll()
        ec.entity.makeFind("service.tracker.JobController").condition("carNo", CarData.carNo).deleteAll()
        ec.entity.makeFind("service.tracker.Technicians").condition("carNo", CarData.carNo).deleteAll()
    }

    def "Check all the car list in Delivery section"() {
        when:
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:CarData.carNo, carStatus:StatusType.inDeliver]).create()
        def getDeliveryList = ec.service.sync().name("tracker.TrackerServices.getDeliveryStatusOfCar").call()

        then:
        (getDeliveryList.deliveryList.carNo).contains(CarData.carNo)
        (getDeliveryList.deliveryList.carStatus).contains(StatusType.inDeliver)

        cleanup:
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", CarData.carNo).deleteAll()
    }

    def "Test for test drive of technician"() {
        when:
        ec.service.sync().name("tracker.TrackerServices.createTestDriveTechnician").parameters([carNo: CarData.carNo, kmIn:CarData.kmIn]).call()
        ec.service.sync().name("tracker.TrackerServices.createTestDriveTechnician").parameters([carNo:CarData.carNo, kmOut:"123456"]).call()
        EntityValue getTotalTestDrive = ec.entity.makeFind("service.tracker.TestDriveTechnician").condition("carNo", CarData.carNo).one()
        then:
        getTotalTestDrive != null
        getTotalTestDrive.testKey != null
        getTotalTestDrive.kmIn == CarData.kmIn
        getTotalTestDrive.kmOut == 123456

        cleanup:
        ec.entity.makeFind("service.tracker.TestDriveTechnician").condition("carNo", CarData.carNo).deleteAll()
    }

    def "Test for Car Delivered to Customer"() {
        when:

        ec.user.setEffectiveTime(MockTime.getTime())
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:CarData.carNo, carStatus:StatusType.inDeliver]).create()
        ec.service.sync().name("tracker.TrackerServices.carDeliveredStatusOfCar").parameters([carNo:CarData.carNo]).call()
        EntityValue getCarDelivered = ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", CarData.carNo).one()

        then:
        getCarDelivered.outTime == MockTime.getTime()

        cleanup:
        ec.user.setEffectiveTime(null)
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", CarData.carNo).deleteAll()
    }

}
