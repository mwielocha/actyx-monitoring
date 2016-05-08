package service

import scala.concurrent.Future
import java.util.UUID

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import play.api.libs.ws._
import play.api.Logger
import play.api.libs.json.Json
import play.api.cache._

import model._
import org.joda.time.DateTime

import akka.stream.scaladsl.Source
import akka.NotUsed
import akka.actor.Props
import akka.stream.actor.ActorPublisher
import akka.actor.ActorLogging
import akka.stream.actor.ActorPublisherMessage._
import akka.stream.scaladsl.Flow

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.pattern._
import akka.util.Timeout

import scala.concurrent.duration._
import scala.language.postfixOps


import actors._

import scala.Either


/**
 * Created by Mikolaj Wielocha on 04/05/16
 */

@Singleton
class MachineParkApiClient @Inject() (
  private val ws: WSClient,
  private val cache: CacheApi)(
  implicit private val ec: ExecutionContext,
  private val actorSystem: ActorSystem) {

  val logger = Logger(getClass)

  val UUIDRegex = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}".r

  val machineUrl = "http://machinepark.actyx.io/api/v1/machine"

  val machineEnpointThrottler = actorSystem.actorOf(Throttler.props(15 millis))

  private implicit val timeout = Timeout(60 seconds)

  def getMachineInfo(machineId: UUID): Future[Either[Throwable, MachineInfo]] = {

    for {
      _ <- machineEnpointThrottler ? Throttler.RequestToken
      result <- ws.url(s"$machineUrl/$machineId").get().map {
        case response if response.status == 200 => Right(MachineInfo(machineId, response.json.as[MachineStatus]))
        case response => logger.error(response.body); Left(new RuntimeException(response.body))
      }
    } yield result

    // Future.successful(Right(MachineInfo(machineId,
    //   MachineStatus("Sample", DateTime.now, math.random), 0.0)))
  }

  val machinesUrl = "http://machinepark.actyx.io/api/v1/machines"

  def getMachines: Future[Either[Throwable, List[UUID]]] = {

    cache.get[List[UUID]]("machines") match {
      case Some(machines) => Future.successful(Right(machines))
      case None =>

        ws.url(machinesUrl).get().map {
          
          case response if response.status == 200 =>
            val machines = response.json.as[List[String]].flatMap {
              url => UUIDRegex.findFirstIn(url)
            }.map(UUID.fromString)

            cache.set("machines", machines, 3 minutes)

            Right(machines)
            
          case response => logger.error(response.body); Left(new RuntimeException(response.body))
        }
      }

    // val raw = """["$API_ROOT/machine/0e079d74-3fce-42c5-86e9-0a4ecc9a26c5","$API_ROOT/machine/e9c8ae10-a943-49e0-979e-71d125132c64","$API_ROOT/machine/95134efd-a6a2-4eb5-9b68-c1bfef18b66c","$API_ROOT/machine/734afe4c-414f-41df-81f3-5953463cfc41","$API_ROOT/machine/f075e663-96af-4761-b2b1-a2ad8d6a791e","$API_ROOT/machine/4f5e573b-5c44-4b0e-8e60-42d1d64d06b1","$API_ROOT/machine/2955159d-d616-4ab8-8c9f-1ed5eb2e6dec","$API_ROOT/machine/5472f532-e693-4402-8090-15a1aa0485cf","$API_ROOT/machine/a8f78f11-9118-413a-8c07-0746becba00f","$API_ROOT/machine/04896c00-3830-4189-81ff-7652d5512fbc","$API_ROOT/machine/fc3def0a-ae5f-4c34-9ec1-eda52d76c821","$API_ROOT/machine/e1fb4637-0cb2-4b10-84fe-325c95edd0f5","$API_ROOT/machine/4d2d7a21-2670-4ced-b0a1-226cb4390076","$API_ROOT/machine/cda1a79c-d4ac-4576-b240-ef58a149ac49","$API_ROOT/machine/6429d438-e1a9-4a13-bc6b-a3995dda6416","$API_ROOT/machine/47da2ecc-87ee-4c18-836c-465ad748f15f","$API_ROOT/machine/9c0d785b-f4b0-46c6-8a64-714fe273345a","$API_ROOT/machine/03a1495f-129e-4618-b5c3-5e95904d9166","$API_ROOT/machine/548708ab-827b-4952-8452-c66f33c0a3ef","$API_ROOT/machine/5cc1f7a7-8ed8-4293-bea6-add619276e60","$API_ROOT/machine/393ecbf5-b91e-46b5-8bd8-e0b8720e6ea8","$API_ROOT/machine/a424cd80-3f2e-4ad5-a119-cacf8a4e96ec","$API_ROOT/machine/ff23b77b-18cd-4b82-924b-dc83a1f678a8","$API_ROOT/machine/18270373-598e-48c6-a894-b37ea42eebc0","$API_ROOT/machine/e74225b8-c5aa-40b9-aad9-efa67d590e5c","$API_ROOT/machine/a43e33e6-59e5-403b-b4a2-ce148de5a2e3","$API_ROOT/machine/9d2b0932-c69e-446a-8d3d-d6ca3747b9f5","$API_ROOT/machine/e381d331-1251-4cf4-bc3b-eb72eee510fa","$API_ROOT/machine/4a70d2fa-42d8-476a-97ef-c63a9f398075","$API_ROOT/machine/ca51eda9-b4fe-406c-9d11-5951a42b2896","$API_ROOT/machine/30120cd8-967c-4c9d-9b10-45e71f203a8e","$API_ROOT/machine/a51f319a-a3f5-42b8-abb8-f8c76aacf0cd","$API_ROOT/machine/7e25fc53-2e41-4a1e-9fab-891a8e8eb01d","$API_ROOT/machine/80a1ea83-27ce-4a26-afcd-82520305be54","$API_ROOT/machine/a6f08cc4-b85e-4f63-a95e-52cce990ebd2","$API_ROOT/machine/ee4c7cb0-f7ae-430b-ba8e-11e52571137a","$API_ROOT/machine/5fd7a15b-698b-4895-8a37-0a1c018cf319","$API_ROOT/machine/835d6852-6d99-48ed-9ba9-a638ad7851c5","$API_ROOT/machine/ccbfc59e-fbff-4e31-b520-91a7e3a861fe","$API_ROOT/machine/6d2fef51-a5d7-4e13-bb95-5b9a429c7a5f","$API_ROOT/machine/9a825a2f-2a43-4d59-be0a-d7a6c8e39db5","$API_ROOT/machine/db7a97fb-bbe4-4b59-94c2-ab2a8b1be17f","$API_ROOT/machine/b981027a-a494-44b1-ad2d-5be75a6c0386","$API_ROOT/machine/b35f5f4e-5ea9-44fc-a740-8260b152f0ca","$API_ROOT/machine/b5431874-ada5-4877-aa7a-9fd3c959f8c2","$API_ROOT/machine/d8ee65d9-aceb-49ac-9f52-94843ed09968","$API_ROOT/machine/6f98bab4-2ec5-4beb-9e1c-0f4c5899e1e9","$API_ROOT/machine/aa81d53b-6487-477b-bde7-887033155632","$API_ROOT/machine/1ed5fb3b-e7c0-475c-a8db-cf3e99a4a3b2","$API_ROOT/machine/21038573-0e47-4377-ab1d-d41e6ea95c69","$API_ROOT/machine/74fe4980-da26-4e7b-a844-1750f952c938","$API_ROOT/machine/69692f92-d7fe-45a1-9abd-1c1751aa1b5b","$API_ROOT/machine/1d503def-b2aa-48a1-822a-93cfbce83629","$API_ROOT/machine/6a386904-f296-4eab-b8f1-68ec6eff2c63","$API_ROOT/machine/be2a5794-0734-436b-8bd8-36fda188624c","$API_ROOT/machine/d9674da8-9e75-4c43-9f07-d156d51891b5","$API_ROOT/machine/584c13cf-6b99-4ab1-b3b4-7745698d0a8a","$API_ROOT/machine/f6ac1407-77f4-4162-b880-05ee3db8b637","$API_ROOT/machine/7eb40e2f-8220-435d-933e-87dcbff7c3d7","$API_ROOT/machine/cd9c7a29-fa79-45e2-9ec8-4632dd7befb8","$API_ROOT/machine/aab83e70-f669-42eb-bd04-4ee96e33d117","$API_ROOT/machine/c34879ce-5111-46ad-b9b3-5c01a9493021","$API_ROOT/machine/3bb21e3b-e5df-4183-b002-e8301a217a6d","$API_ROOT/machine/6be7824f-3303-4c89-a910-6f162ae768e9","$API_ROOT/machine/214cacba-4045-41df-aa51-896415fe1448","$API_ROOT/machine/0bd4606b-b3b8-4584-98bf-3127728e2c0a","$API_ROOT/machine/f67c634e-0389-4d34-b5e0-a4e2d3b55a9e","$API_ROOT/machine/b051c54d-2996-47d2-8cfa-f213cdb2dabb","$API_ROOT/machine/774ba648-8a28-4509-b9aa-ef39c8893817","$API_ROOT/machine/20f94d65-6f3e-432f-b448-510b12f90eba","$API_ROOT/machine/83ea1549-01c6-484c-abb8-b77aa4468680","$API_ROOT/machine/ef691047-6aa3-4087-99ba-6b371a17b2ec","$API_ROOT/machine/82f29969-aff7-42b8-a2f5-a98527bd3bfe","$API_ROOT/machine/cfc1be5f-1b65-4b35-8a94-222131a2e653","$API_ROOT/machine/7e39c7d2-85d8-4a10-8e62-9cf192693d6c","$API_ROOT/machine/c09dacc3-ce7a-4f79-85f7-1838a39b5876","$API_ROOT/machine/918cf953-a32f-4d63-a2a4-f3a7a32712f6","$API_ROOT/machine/5f38262e-93e3-4729-9e5a-2dfe90863714","$API_ROOT/machine/d1bf1882-900c-4bd4-b337-f2be73d69bba","$API_ROOT/machine/15e6cf51-8b51-47f2-86f3-84e4f2b6813c","$API_ROOT/machine/80c561b5-1bb7-42ac-8374-3342a2ffb06c","$API_ROOT/machine/05d08583-e180-4f23-9420-d0d27124106e","$API_ROOT/machine/d3f73289-90f9-4b61-84af-e0987aa8259d","$API_ROOT/machine/a8e216a5-2781-4869-b1a6-9d341e35a3ec","$API_ROOT/machine/7a5d54f3-aadd-4781-b9f1-a3d76694014f","$API_ROOT/machine/cef33852-21b8-4635-b5ab-aaba4826adcd","$API_ROOT/machine/e5793b3d-f8c3-4e84-adcd-e4b47bcc9bd2","$API_ROOT/machine/57dda61f-6cf9-44bb-8368-2ac241214d66","$API_ROOT/machine/2e1f3614-bc24-4540-bd5d-d78325029770","$API_ROOT/machine/84a47263-3aef-4a81-adcf-3b86935c45c8","$API_ROOT/machine/c2d0b7b8-df68-4ee1-8a41-30b26a748783","$API_ROOT/machine/52cf7c5d-e3c5-45b7-854a-91cf3ebdabf4","$API_ROOT/machine/137af1f0-5e3a-4178-93bc-51c89127fb42","$API_ROOT/machine/ebbcbf40-0544-4734-a3cc-42d5c01c3c29","$API_ROOT/machine/2a8da0a0-d640-4e5f-b537-8b0f4094cc48","$API_ROOT/machine/1aa4ea30-21a6-45c5-b039-f2243590402f","$API_ROOT/machine/d77c8580-92b5-4235-9b1c-c1a8f972b8ae","$API_ROOT/machine/cedbd2a8-111a-4235-8ee8-09f7ccbff7d4","$API_ROOT/machine/049c8e41-f883-4398-a7f1-d7765d46fb74","$API_ROOT/machine/656aeff3-226a-492c-933e-4d1f7c289222","$API_ROOT/machine/65a764e2-2094-4045-9e04-e55aa2ce5792","$API_ROOT/machine/f3bef5b2-efce-4069-80ef-36c74a03677f","$API_ROOT/machine/e405bd1e-8c00-49f0-9bc5-e304efb79e66","$API_ROOT/machine/54b02e30-b23f-479b-b4d3-9ecd77d9cffd","$API_ROOT/machine/c0bc459a-d098-4214-9e3b-f140b48b7a16","$API_ROOT/machine/19f0df5d-97f0-4250-92d2-3f02729ec127","$API_ROOT/machine/6022c493-49b8-4625-b5de-67ade479f8db","$API_ROOT/machine/2525e67f-594d-4521-8351-9bbf88cee09d","$API_ROOT/machine/eb564ff4-9cb7-4b86-8283-6e87f5d62251","$API_ROOT/machine/16863ac7-b61d-4a50-8599-ebf791073795","$API_ROOT/machine/ef415317-8a89-466c-bc2c-fe59fe4e984b","$API_ROOT/machine/69091c26-8018-4e95-b6bf-f79dd0f9a6c5","$API_ROOT/machine/e429f5d9-ebe5-4dfb-a32b-7dd5fff563c3","$API_ROOT/machine/0b782ac6-d288-476e-81c9-4e75621982f2","$API_ROOT/machine/af750c77-dc5b-4c8b-96d4-284c368327bb","$API_ROOT/machine/f84610f8-6940-4276-ac44-66812d5c0c9f","$API_ROOT/machine/740ca072-b0f9-4ed0-bdcf-9bc00c950ff6","$API_ROOT/machine/1710245c-5e4f-48d6-b9fd-f49a56e5270a","$API_ROOT/machine/6744ece8-18fb-4e8e-9be2-517522156505","$API_ROOT/machine/e6bf5166-843a-49b3-a01a-13003d59f2c8","$API_ROOT/machine/91e6ee62-466e-422e-988d-51abc6acbc93","$API_ROOT/machine/ab155b36-6df3-4758-acee-60f374b5014d","$API_ROOT/machine/cd6213ab-fb2c-4864-9e19-cb22cd4f2495","$API_ROOT/machine/4cf6cd16-1f65-49d1-8c8f-a0aac44f3472","$API_ROOT/machine/f91c80fe-5568-41dc-b2aa-178281bca1a5","$API_ROOT/machine/68fa9508-17cc-42fb-b1b0-fde543af4ec2","$API_ROOT/machine/c5eafb81-c96a-4220-8c9b-00c998b06d8e","$API_ROOT/machine/378acc46-777d-4b48-8205-91f982bef2d0","$API_ROOT/machine/faeac2e0-7970-46d6-bd79-23e0a6614d17","$API_ROOT/machine/edfeef10-f301-4331-8407-1bccbc2142df","$API_ROOT/machine/fd289c78-b25a-4959-8879-4188dbc03d68","$API_ROOT/machine/55f3ab6e-2e83-4001-a077-6679021f5657","$API_ROOT/machine/68767c84-60ba-4940-821b-67455356ca57","$API_ROOT/machine/6fb054aa-6a48-4274-a077-ac829ec8f764","$API_ROOT/machine/4f113e4f-319a-4871-aed2-3bb85e21d2e7","$API_ROOT/machine/81bc3bd8-99ae-4adc-af9a-d4fe497e0c6d","$API_ROOT/machine/1edca4ea-9321-4c46-b171-930306e59ed0","$API_ROOT/machine/e71b6add-d37f-4e58-8cef-0138a128245b","$API_ROOT/machine/56716afc-901b-407a-aee0-7d75fe28363d","$API_ROOT/machine/4e83dd8c-9e10-42ca-9060-4462ae29afd1","$API_ROOT/machine/d55f1545-caf6-4f9f-a31f-7fa1851e4846","$API_ROOT/machine/ae563d66-1bad-4f3d-a893-3046bda883f7","$API_ROOT/machine/5731d38c-86cf-412f-ae51-f990165f0e95","$API_ROOT/machine/68233f54-b430-4b93-bedb-7c309625fef7","$API_ROOT/machine/e7335741-e93a-4a30-b430-7cf48b8d4afd","$API_ROOT/machine/eb1c0e56-6ef4-4eb8-ae50-74da75a8c660","$API_ROOT/machine/b09ad077-3c50-458d-90f1-9ee9fc397af7","$API_ROOT/machine/320bed8f-2fb4-45f9-949c-f88acf76fa90","$API_ROOT/machine/3bb78081-350f-4de8-88e3-10dda3483227","$API_ROOT/machine/8d30fc0f-98f3-4e24-9f49-81fb91d94fa8","$API_ROOT/machine/94af0f02-5f50-4d0a-9794-029fa3d62016","$API_ROOT/machine/0f0db8d5-65ae-4f30-96a5-5783d59f7fa7","$API_ROOT/machine/00c2da19-1634-476d-b113-0c2a9f10fbb0","$API_ROOT/machine/582616bc-2a4f-41e6-819c-ac1a881cd584","$API_ROOT/machine/26b93369-20f3-4b4d-8fe9-d6500da1c6bd","$API_ROOT/machine/82b1a135-e94c-4a82-b313-4c5b8ff68d13","$API_ROOT/machine/7e4920d7-b328-4974-a84e-cd01c8c03c20","$API_ROOT/machine/55228e55-16a1-4a33-b6d4-67ed40188275","$API_ROOT/machine/36e4e54e-878d-4c28-a236-a7ad4adec310","$API_ROOT/machine/53302109-78ab-4d92-bac0-ac38af229f01","$API_ROOT/machine/9bb1134c-653c-4c69-bbaa-210968f594fa","$API_ROOT/machine/f018759a-f5af-4037-9737-c280ed3ee86d","$API_ROOT/machine/0d119a07-b8c8-4034-829c-a746bca71a13","$API_ROOT/machine/c5497a79-5f5f-45b8-99fa-ab4b80951d5b","$API_ROOT/machine/0f35bc7c-147b-49dd-a3e7-283f3585e9d6","$API_ROOT/machine/2907c984-91a2-4d17-ad21-7cf13dc9241e","$API_ROOT/machine/4c2c09ee-ce0f-4bd7-a25e-3f6acd8cb495","$API_ROOT/machine/bff023a6-a303-494d-80d9-8cf3204847ca","$API_ROOT/machine/2e7f3bf0-7bb4-4c00-9294-c523730e01e8","$API_ROOT/machine/0e1900e0-3bb9-4350-88a5-8a331b03e8ce","$API_ROOT/machine/6503d086-8317-40d3-90fa-5e4bf678f195","$API_ROOT/machine/f6df9b3c-d801-407d-9fe2-8e8da81a2a1e","$API_ROOT/machine/06b6dbc6-6c1b-42fe-8d37-afaa705a5a6e","$API_ROOT/machine/2d808c08-4c2a-42ed-ad38-70696a24677b","$API_ROOT/machine/f2c200c2-3cea-469d-83c8-7d93e3869cb3","$API_ROOT/machine/f048c3c4-beea-46b5-9810-915b1da0093a","$API_ROOT/machine/99cc4953-cbb5-4736-afb7-fe1eb0c1a7d3","$API_ROOT/machine/c2a0b5a5-c839-456e-81e1-d2f31da79e3d","$API_ROOT/machine/c49fa086-cf9d-40b5-8bcc-620602bdf0e6","$API_ROOT/machine/4b1ace6a-d89a-4f27-8ab3-ea4509d86350","$API_ROOT/machine/1455f916-5986-4c84-a7c9-547dd1c2a7d5","$API_ROOT/machine/093f5200-592a-429c-97e7-38bc9e00b52b","$API_ROOT/machine/ce448684-a85b-42bb-bac7-71dddc79ed19","$API_ROOT/machine/5721d8e7-7d4a-46c1-9a24-74dcdfa6bfe2","$API_ROOT/machine/123e7d73-ac0b-4642-91f6-faf47c62cfa7","$API_ROOT/machine/1eaf7c32-a4e3-49e6-9270-d6fb5aac3b03","$API_ROOT/machine/6de14c2e-1e7f-4939-8718-ed0b24920225","$API_ROOT/machine/a34306f0-a3b1-4071-82a5-ea8d7a35c0cf","$API_ROOT/machine/25f85ddd-11a4-4884-9b15-67332fcc71f4","$API_ROOT/machine/a3b1e851-2d2b-4f63-9d6b-9825ea852b12","$API_ROOT/machine/508a1520-1324-4551-8db5-9886add2a934","$API_ROOT/machine/0b23febd-db60-4531-941a-84cc91a62e7b","$API_ROOT/machine/60c27f75-5871-4499-b3e8-9d2a91cec04b","$API_ROOT/machine/76bc3804-95ea-4640-a990-dad77511e53a","$API_ROOT/machine/f4da2605-9442-4fbe-802f-0f00a23793b5","$API_ROOT/machine/3ba9a339-37e3-4e6f-8101-427f38ee0c8a","$API_ROOT/machine/e63fbd8a-20e4-427b-8d8b-79133d9cb5aa","$API_ROOT/machine/2ebde7b0-d26c-4ae7-8ccc-3fdacd26cea8","$API_ROOT/machine/4e6dcc77-3956-4b0c-8ddd-beb4920f0c82","$API_ROOT/machine/e74e19a7-e0b5-4974-a7e7-508b48660964","$API_ROOT/machine/d051163b-5806-4592-b760-fa5f9187a7bb","$API_ROOT/machine/026de656-3002-44db-8376-de2233541383","$API_ROOT/machine/b972bf76-8b58-49f6-a585-6a3ccdc4ca7b","$API_ROOT/machine/d6724ccb-2933-4310-9074-ef0e50816b68","$API_ROOT/machine/7705362b-7e3b-4a3e-a242-806204f32001","$API_ROOT/machine/c28bd870-d173-47d7-b20d-baeec1d00418","$API_ROOT/machine/62460624-7b29-4bf7-a72e-62e8a856570f","$API_ROOT/machine/c9566a6a-1e9e-4d46-ab77-afcefa7c89be","$API_ROOT/machine/6bc619c0-b185-40ce-aeb0-976ad46d4d94","$API_ROOT/machine/7a0db43b-76a4-4baa-81f7-50e305e6c376","$API_ROOT/machine/70ada504-1bcb-4085-8bbd-190dabd6929d","$API_ROOT/machine/5c3e18e9-d4cb-40f5-9155-286dfb499bc7","$API_ROOT/machine/112935e5-ad3d-4e91-b213-f1dd6f2aacd5","$API_ROOT/machine/ac31208b-7774-407f-b8cf-8cb5f5c788d1","$API_ROOT/machine/ce2eef24-4eae-49ee-ab9f-9596dfcc074a","$API_ROOT/machine/a12e8772-1810-4f23-b8ba-9e7614fcad4b","$API_ROOT/machine/c346088d-419f-45b2-9f06-fe5cabcd0f30","$API_ROOT/machine/efe1438a-9912-4e45-92b3-7a3550065ac6","$API_ROOT/machine/69972be7-dfbb-4166-9c82-efd7b3d6db9f","$API_ROOT/machine/84da6dda-3b79-4963-a0ec-b1fd30001bda","$API_ROOT/machine/3c2ce088-9aa2-43d0-9996-9801ce2b4124","$API_ROOT/machine/14985de7-b984-4b5f-9133-1a13e44f4361","$API_ROOT/machine/f164a59d-3773-43cc-8466-bb0ad92642cd","$API_ROOT/machine/7708fecb-ce54-4a58-b762-47a670161bd4","$API_ROOT/machine/867216e0-8d0a-4f9e-9f5a-46c54a0aace9","$API_ROOT/machine/3e84e40b-ca45-48e5-b13a-9bc059e067e8","$API_ROOT/machine/d05b4b1d-2c8b-4304-a274-4ebe2b327dbf","$API_ROOT/machine/51dcad57-3c08-4a4e-b7b2-3f852a41cabd","$API_ROOT/machine/14b84b94-dd7c-48ac-8d5c-b99ed658dd0d","$API_ROOT/machine/4fa356c4-1809-4aab-aba6-4e9927b2c509","$API_ROOT/machine/263263ff-004d-4d3d-8758-7e4d6be04c9b","$API_ROOT/machine/41163b7b-b8f0-43e6-b24d-efc541ffef9e","$API_ROOT/machine/0e9b70c3-fe48-40f2-aac6-f4b9ab466b91","$API_ROOT/machine/af92bbb9-cf77-4655-a3dc-b120bdd42127","$API_ROOT/machine/ff0e34c0-302d-4cfb-98d5-cc8b29c1e650","$API_ROOT/machine/d6a2d725-2e14-46f9-beb9-5629282e6fec","$API_ROOT/machine/21bc56e3-1d40-4051-80f0-5d8a2f8db303","$API_ROOT/machine/f4141adf-dce7-466e-8255-53db85fb1ce4","$API_ROOT/machine/9fbd81cf-af67-4a83-b2c3-f9d62ba34570","$API_ROOT/machine/e3369df7-e117-4c18-a460-a4dd43e1eeb9","$API_ROOT/machine/bf0d6107-b65b-440f-ab5d-05566158e185","$API_ROOT/machine/233fab40-4cdb-4030-b854-d066093d273e","$API_ROOT/machine/b6d6ea8a-ef58-4ff6-8265-4fc0ddf4c7ec"]"""
    // 
    // Future.successful(Right(
    //   Json.parse(raw).as[List[String]]
    //     .flatMap(UUIDRegex.findFirstIn(_))
    //     .map(UUID.fromString)
    // ))
  }

  def newMachineInfoSource(machineId: UUID): Source[MachineInfo, _] = {
    Source.actorPublisher(Props(new AsyncPublisher[MachineInfo]( () => getMachineInfo(machineId))))
  } 

  val environmentalSensorUrl = "http://machinepark.actyx.io/api/v1/env-sensor"

  def getEnvironmentalInfo: Future[Either[Throwable, EnvironmentalInfo]] = {
    ws.url(environmentalSensorUrl).get().map {
      case response if response.status == 200 => Right(response.json.as[EnvironmentalInfo])
      case response => logger.error(response.body); Left(new RuntimeException(response.body))
     }

    // val now = DateTime.now
    // 
    // Future.successful(Right(
    //   EnvironmentalInfo(
    //     Metric(math.random, now),
    //     Metric(math.random, now),
    //     Metric(math.random, now)
    //   )
    //))
  }

  def newEnvironmentalInfoSource: Source[EnvironmentalInfo, _] = Source.actorPublisher {
    Props(new AsyncPublisher[EnvironmentalInfo](() => getEnvironmentalInfo))
  }

  private [MachineParkApiClient] class AsyncPublisher[T](getAsync: () => Future[Either[Throwable, T]])
      extends ActorPublisher[T] with ActorLogging {

    import context.dispatcher

    def receive = {

      case Request(n) =>

        getAsync().foreach {
          case Left(t) => onError(t)
          case Right(e) => onNext(e)
        }

      case Cancel => context.stop(self)
    }
  }
}

