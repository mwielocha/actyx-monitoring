import play.api.libs.json._

/**
 * Created by Mikolaj Wielocha on 06/05/16
 */

package object model {

 implicit val jodaDateReads = Reads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")

}
