import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityValue
import spock.lang.*
import spock.lang.Specification

import javax.swing.text.html.parser.Entity


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
    def "Create car entry with Reception Entity service"(){
        when:
        def createEntry = ec.service.sync().name("tracker.TrackerServices.createReceptionEntity")
                                           .parameters([CarNo : "WB-02_M4", Job : "PM(Free)", ServiceAdvisor : "Abhishek Bagchi", InTime : "2014-10-08 21:20:00.0", DriverOrOwner : "Owner",
                                                      CarStatus : "Reception" , Gift : "Yes", DropCar : "Yes", CustomerWaiting : "No"]).call()
        EntityValue carCreated = ec.entity.makeFind("service.tracker.ReceptionEntity").condition("CarNo", "WB-02_M4").one()
        EntityValue status = ec.entity.makeFind("service.tracker.CarNoAndStatus").condition("CarNo", "WB-02_M4").one()
        then:
        carCreated != null
        carCreated.Job == "PM(Free)"
        carCreated.ServiceAdvisor == "Abhishek Bagchi"
        carCreated.DriverOrOwner == "Owner"
        carCreated.Gift == "Yes"
        carCreated.DropCar == "Yes"
        carCreated.CustomerWaiting == "No"
        status.CarStatus == "Reception"
        cleanup:
        carCreated.delete()
        status.delete()
    }

   /* def "Test Entity ECA rule for Reception Entity service"(){
        when:
        def createEntry = ec.service.sync().name("tracker.TrackerServices.createReceptionEntity")
                .parameters([CarNo : "frtc", Job : "PM(Free)", ServiceAdvisor : "Abhishek Bagchi", InTime : "2014-10-08 21:20:00.0", DriverOrOwner : "Owner"
                , Gift : "Yes", CarStatus : "Reception", DropCar : "Yes", CustomerWaiting : "No"]).call()
        EntityValue getStatus = ec.entity.makeFind("service.tracker.CarNoAndStatus").condition("CarNo", "frtc").one()
        then:
        getStatus != null
        getStatus.CarStatus == "Reception"
        cleanup:
       getStatus.delete()
    } */
 /*   def "delete Record"(){
        when:
        def deleteRecord  = ec.entity.makeFind("service.tracker.ReceptionEntity").condition("CarNo", "WB-02-H-8917").one()
        deleteRecord.
        def findRecord = ec.entity.makeFind("service.tracker.ReceptionEntity").condition("CarNo", "WB-02-H-8917").one()
        then:
        findRecord == null
    } */

}