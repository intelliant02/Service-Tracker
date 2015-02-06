import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityValue
import spock.lang.Shared
import spock.lang.Specification
import testdata.*

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
                .parameters([carNo: CarData.CAR_NO, kmIn: CarData.KM_IN]).call()
        EntityValue getValue = ec.entity.makeFind("service.tracker.SecurityCheck").condition("carNo", CarData.CAR_NO).one()
        EntityValue statusValue = ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", CarData.CAR_NO).one()
        then:
        getValue != null
        statusValue != null
        getValue.kmIn == CarData.KM_IN
        statusValue.carStatus == StatusType.inReception
        statusValue.inTime == MockTime.getTime()

        cleanup:
        ec.user.setEffectiveTime(null)
        getValue.delete()
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", CarData.CAR_NO).deleteAll()
    }

    def "Test Reception Entry"() {
        when:
        ec.user.setEffectiveTime(MockTime.getTime())
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:CarData.CAR_NO, carStatus:StatusType.inReception]).create()
        ec.service.sync().name("tracker.TrackerServices.createReceptionEntity")
                .parameters([carNo: CarData.CAR_NO, job: CarService.JOB, serviceAdviser: CarService.ADVISER, driverOrOwner: "Owner",
                             gift: Decision.sayYes(), dropCar: Decision.sayYes(), customerWaiting: Decision.sayNo()]).call()
        EntityValue getValue = ec.entity.makeFind("service.tracker.ReceptionEntity").condition("carNo", CarData.CAR_NO).one()
        EntityValue getStatusOfCar = ec.entity.makeFind("service.tracker.StatusOfCar").condition([carNo:CarData.CAR_NO, carStatus:StatusType.inAdviser]).one()

        then:
        getValue.job == CarService.JOB
        getValue.serviceAdviser == CarService.ADVISER
        getValue.driverOrOwner == "Owner"
        getValue.gift == Decision.sayYes()
        getValue.dropCar == Decision.sayYes()
        getValue.customerWaiting == Decision.sayNo()
        getStatusOfCar.inTime == MockTime.getTime()

        cleanup:
        ec.user.setEffectiveTime(null)
        ec.entity.makeFind("service.tracker.ReceptionEntity").condition("carNo", CarData.CAR_NO).deleteAll()
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", CarData.CAR_NO).deleteAll()
    }

    def "Check all the car list in reception section"() {
        when:
        ec.service.sync().name("tracker.TrackerServices.createSecurityCheck")
                .parameters([carNo: CarData.CAR_NO, kmIn: CarData.KM_IN]).call()
        Map getReceptionList = ec.service.sync().name("tracker.TrackerServices.getSecurityAndCarStatus").call()

        then:
        (getReceptionList.receptionCarList.carNo).contains(CarData.CAR_NO)
        (getReceptionList.receptionCarList.kmIn).last() == CarData.KM_IN

        cleanup:
        ec.entity.makeFind("service.tracker.SecurityCheck").condition("carNo", CarData.CAR_NO).deleteAll()
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", CarData.CAR_NO).deleteAll()
    }

    def "Check all the car list in Adviser section"() {
        when:
        ec.entity.makeValue("service.tracker.ReceptionEntity").setAll([carNo:CarData.CAR_NO, job:CarService.JOB, serviceAdviser:CarService.ADVISER, driverOrOwner:"Owner",
                                                                       gift: Decision.sayYes(), dropCar: Decision.sayYes(), customerWaiting: Decision.sayNo()]).create()
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:CarData.CAR_NO, carStatus:StatusType.inAdviser]).create()
        def getAdviserList = ec.service.sync().name("tracker.TrackerServices.getReceptionEntityAndCarStatus").call()

        then:
        (getAdviserList.adviserCarList.carNo).contains(CarData.CAR_NO)
        (getAdviserList.adviserCarList.serviceAdviser).contains(CarService.ADVISER)
        (getAdviserList.adviserCarList.carStatus).contains(StatusType.inAdviser)

        cleanup:
        ec.entity.makeFind("service.tracker.ReceptionEntity").condition("carNo", CarData.CAR_NO).deleteAll()
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", CarData.CAR_NO).deleteAll()
    }

    def "Remove car from washing section which is ready for delivered"() {
        when:
        ec.user.setEffectiveTime(MockTime.getTime())
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:CarData.CAR_NO, carStatus:StatusType.inWasher, inTime:ec.user.getNowTimestamp()]).create()
        ec.service.sync().name("tracker.TrackerServices.doneWashingStatusOfCar").parameters([carNo:CarData.CAR_NO]).call()
        def nullWashingCar = ec.entity.makeFind("service.tracker.StatusOfCar").condition([carNo:CarData.CAR_NO, outTime:null, carStatus:StatusType.inWasher]).list()
        def getWashingCar = ec.entity.makeFind("service.tracker.StatusOfCar").condition([carNo:CarData.CAR_NO]).one()

        then:
        nullWashingCar.isEmpty()
        getWashingCar.inTime == MockTime.getTime()
        getWashingCar.carStatus == StatusType.inDeliver

        cleanup:
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", CarData.CAR_NO).deleteAll()
        ec.user.setEffectiveTime(null)
    }

    def "Creation of Job Controller"() {
        when:
        ec.user.setEffectiveTime(MockTime.getTime())
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:CarData.CAR_NO, carStatus:StatusType.inController]).create()
        ec.service.sync().name("tracker.TrackerServices.createJobController")
                .parameters([carNo:CarData.CAR_NO , area:"Ground floor", bayNo:"2", technicianId: CarService.TECHNICIAN, awaitingTechnician: Decision.sayYes(),
                             jobId:"job1"]).call()
        EntityValue getJobController = ec.entity.makeFind("service.tracker.JobController").condition("carNo", CarData.CAR_NO).one()
        EntityValue getOutStatus = ec.entity.makeFind("service.tracker.StatusOfCar").condition([carNo:CarData.CAR_NO, carStatus:StatusType.inController]).one()
        EntityValue getTechnician = ec.entity.makeFind("service.tracker.Technicians").condition("carNo", CarData.CAR_NO).one()
        EntityValue getStatus = ec.entity.makeFind("service.tracker.StatusOfCar").condition([carNo:CarData.CAR_NO, carStatus:StatusType.inTechnician]).one()
        then:
        getJobController != null
        getJobController.awaitingTechnician == Decision.sayYes()
        getTechnician != null
        getTechnician.technicianId == CarService.TECHNICIAN
        getStatus.inTime == MockTime.getTime()
        getOutStatus.outTime == MockTime.getTime()

        cleanup:
        ec.user.setEffectiveTime(null)
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", CarData.CAR_NO).deleteAll()
        ec.entity.makeFind("service.tracker.JobController").condition("carNo", CarData.CAR_NO).deleteAll()
        ec.entity.makeFind("service.tracker.Technicians").condition("carNo", CarData.CAR_NO).deleteAll()
        def updateCarPlace = ec.entity.makeFind("service.internal.CarPlaces").condition("placeValue", "Ground floor 2").one()
        updateCarPlace.vacant = "0"
        updateCarPlace.update()
    }

    def "Test Service Adviser Entry"() {
        when:
        ec.user.setEffectiveTime(MockTime.getTime())
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:CarData.CAR_NO, carStatus:StatusType.inAdviser]).create()
        ec.service.sync().name("tracker.TrackerServices.createAdviserEntry")
                .parameters([carNo:CarData.CAR_NO, customerName:"Deb", mobileNo:"9836545651",
                             driverName:"Raj", driverMobile:"9836545651",
                             job:CarService.JOB, beforeRoadTest:Decision.sayYes(), afterRoadTest:Decision.sayNo(),
                             capNumber: "1234567", promisedTime:"2014-11-27 11:55"]).call()
        EntityValue getAdviser = ec.entity.makeFind("service.tracker.AdviserEntry").condition("carNo", CarData.CAR_NO).one()
        EntityValue getOutStatus = ec.entity.makeFind("service.tracker.StatusOfCar").condition([carNo:CarData.CAR_NO, carStatus:StatusType.inAdviser]).one()
        EntityValue getInStatus = ec.entity.makeFind("service.tracker.StatusOfCar").condition([carNo:CarData.CAR_NO, carStatus:StatusType.inController]).one()

        then:
        getAdviser != null
        getInStatus.inTime == MockTime.getTime()
        getOutStatus.outTime == MockTime.getTime()
        getAdviser.customerName == "Deb"
        getAdviser.mobileNo == "9836545651"
        getAdviser.job == CarService.JOB
        getAdviser.beforeRoadTest == Decision.sayYes()
        getAdviser.afterRoadTest == Decision.sayNo()
        getAdviser.promisedTime != null

        cleanup:
        ec.user.setEffectiveTime(null)
        ec.entity.makeFind("service.tracker.AdviserEntry").condition("carNo", CarData.CAR_NO).deleteAll()
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", CarData.CAR_NO).deleteAll()
    }


    def "Check all the car list in Job Controller section"() {
        when:
        ec.entity.makeFind("service.tracker.AdviserEntry").condition("carNo", CarData.CAR_NO).deleteAll()
        ec.entity.makeValue("service.tracker.AdviserEntry").setAll([carNo:CarData.CAR_NO, customerName:"Deb", mobileNo:"9836545651",
                                                                    driverName:"Raj", driverMobile:"9836545651",
                                                                    job:CarService.JOB, beforeRoadTest:Decision.sayYes(), afterRoadTest:Decision.sayNo(), capNumber: CarService.CAP]).create()
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:CarData.CAR_NO, carStatus:StatusType.inController]).create()
        Map getControllerList = ec.service.sync().name("tracker.TrackerServices.getAdviserEntryAndCarStatus").call()

        then:
        getControllerList.jobControllerCarList.carNo.contains(CarData.CAR_NO)
        getControllerList.jobControllerCarList.customerName.contains("Deb")
        getControllerList.jobControllerCarList.mobileNo.contains("9836545651")
        getControllerList.jobControllerCarList.driverName.contains("Raj")
        getControllerList.jobControllerCarList.driverMobile.contains("9836545651")
        getControllerList.jobControllerCarList.job.contains(CarService.JOB)
        getControllerList.jobControllerCarList.beforeRoadTest.contains(Decision.sayYes())
        getControllerList.jobControllerCarList.afterRoadTest.contains(Decision.sayNo())
        getControllerList.jobControllerCarList.carStatus.contains(StatusType.inController)

        cleanup:
        ec.entity.makeFind("service.tracker.AdviserEntry").condition("carNo", CarData.CAR_NO).deleteAll()
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", CarData.CAR_NO).deleteAll()
    }

    def "Check all the car list in washing section"() {
        when:
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:CarData.CAR_NO, carStatus:StatusType.inWasher]).create()
        def washingCarList = ec.service.sync().name("tracker.TrackerServices.getWashingStatusOfCar").call()

        then:
        washingCarList != null
        (washingCarList.washingList.carNo).contains(CarData.CAR_NO)
        (washingCarList.washingList.carStatus).contains(StatusType.inWasher)

        cleanup:
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", CarData.CAR_NO).deleteAll()
    }

    def "Check all the car list in Get delivery section"() {
        when:
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:CarData.CAR_NO, carStatus:StatusType.inDeliver]).create()
        def deliveryCarList = ec.service.sync().name("tracker.TrackerServices.getDeliveryStatusOfCar").call()

        then:
        (deliveryCarList.deliveryList.carNo).contains(CarData.CAR_NO)
        (deliveryCarList.deliveryList.carStatus).contains(StatusType.inDeliver)

        cleanup:
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", CarData.CAR_NO).deleteAll()
    }

    def "Check all the car list in Technician section"() {
        when:
        ec.entity.makeValue("service.tracker.JobController").setAll([carNo:CarData.CAR_NO , area:"Ground floor", bayNo:"1", awaitingTechnician: Decision.sayYes(),
                                                                     fromDate:ec.user.getNowTimestamp()]).create()
        ec.entity.makeValue("service.tracker.Technicians").setAll([carNo:CarData.CAR_NO, jobId:"Job1", technicianId: CarService.TECHNICIAN, fromDate:ec.user.getNowTimestamp()]).create()

        ec.entity.makeValue("service.tracker.JobController").setAll([carNo:CarData.CAR_NO , area:"First floor", bayNo:"1", awaitingTechnician: Decision.sayYes(),
                                                                     fromDate:ec.user.getNowTimestamp()]).create()
        ec.entity.makeValue("service.tracker.Technicians").setAll([carNo:CarData.CAR_NO, jobId:"Job2", technicianId:"12345", fromDate:ec.user.getNowTimestamp()]).create()
        def getTechnicianList = ec.service.sync().name("tracker.TrackerServices.getJobControllerAndTechnician").call()
        def getFirstFloorTechnicianList = ec.service.sync().name("tracker.TrackerServices.getJobControllerAndTechnician").call()
        then:
        (getTechnicianList.technicianList.carNo).contains(CarData.CAR_NO)
        (getTechnicianList.technicianList.area).contains("Ground floor")
        (getTechnicianList.technicianList.bayNo).contains("1")
        (getTechnicianList.technicianList.technicianId).contains("123456")
        (getTechnicianList.technicianList.awaitingTechnician).contains(Decision.sayYes())
        (getTechnicianList.technicianList.jobId).contains("Job1")
        (getFirstFloorTechnicianList.technicianListFirstFloor.carNo).contains(CarData.CAR_NO)
        (getFirstFloorTechnicianList.technicianListFirstFloor.area).contains("First floor")
        (getFirstFloorTechnicianList.technicianListFirstFloor.bayNo).contains("1")
        (getFirstFloorTechnicianList.technicianListFirstFloor.technicianId).contains("12345")
        (getFirstFloorTechnicianList.technicianListFirstFloor.awaitingTechnician).contains(Decision.sayYes())
        (getFirstFloorTechnicianList.technicianListFirstFloor.jobId).contains("Job2")

        cleanup:
        ec.entity.makeFind("service.tracker.JobController").condition("carNo", CarData.CAR_NO).deleteAll()
        ec.entity.makeFind("service.tracker.Technicians").condition("carNo", CarData.CAR_NO).deleteAll()
        ec.entity.makeFind("service.tracker.JobController").condition("carNo", CarData.CAR_NO).deleteAll()
        ec.entity.makeFind("service.tracker.Technicians").condition("carNo", CarData.CAR_NO).deleteAll()
    }

    def "Check all the car list in Delivery section"() {
        when:
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:CarData.CAR_NO, carStatus:StatusType.inDeliver]).create()
        def getDeliveryList = ec.service.sync().name("tracker.TrackerServices.getDeliveryStatusOfCar").call()

        then:
        (getDeliveryList.deliveryList.carNo).contains(CarData.CAR_NO)
        (getDeliveryList.deliveryList.carStatus).contains(StatusType.inDeliver)

        cleanup:
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", CarData.CAR_NO).deleteAll()
    }

    def "Test for test drive of technician"() {
        when:
        ec.service.sync().name("tracker.TrackerServices.createTestDriveTechnician").parameters([carNo: CarData.CAR_NO, kmIn:CarData.KM_IN]).call()
        ec.service.sync().name("tracker.TrackerServices.createTestDriveTechnician").parameters([carNo:CarData.CAR_NO, kmOut: CarData.KM_OUT]).call()
        EntityValue getTotalTestDrive = ec.entity.makeFind("service.tracker.TestDriveTechnician").condition("carNo", CarData.CAR_NO).one()
        then:
        getTotalTestDrive != null
        getTotalTestDrive.testKey != null
        getTotalTestDrive.kmIn == CarData.KM_IN
        getTotalTestDrive.kmOut == CarData.KM_OUT

        cleanup:
        ec.entity.makeFind("service.tracker.TestDriveTechnician").condition("carNo", CarData.CAR_NO).deleteAll()
    }

    def "Test for Car Delivered to Customer"() {
        when:

        ec.user.setEffectiveTime(MockTime.getTime())
        ec.entity.makeValue("service.tracker.StatusOfCar").setAll([carNo:CarData.CAR_NO, carStatus:StatusType.inDeliver]).create()
        ec.service.sync().name("tracker.TrackerServices.carDeliveredStatusOfCar").parameters([carNo:CarData.CAR_NO]).call()
        EntityValue getCarDelivered = ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", CarData.CAR_NO).one()

        then:
        getCarDelivered.outTime == MockTime.getTime()

        cleanup:
        ec.user.setEffectiveTime(null)
        ec.entity.makeFind("service.tracker.StatusOfCar").condition("carNo", CarData.CAR_NO).deleteAll()
    }
}
