package wordstrainer

import java.io.FileInputStream
import java.util.Properties

private case class Settings private (
    dataDir: String,
    googleSecrets: GoogleSecrets
)

private case class GoogleSecrets private (
    xt: String,
    sid: String,
    hsid: String,
    ssid: String
)

private object Settings {

  private val fileName = "settings.properties"

  def apply(): Settings = {
    val fis = new FileInputStream(fileName)
    val props =
      try {
        val props = new Properties()
        props.load(fis)
        props
      } finally {
        fis.close()
      }

    val googleSecrets = GoogleSecrets(
      getOrThrow(props, "xt"),
      getOrThrow(props, "SID"),
      getOrThrow(props, "HSID"),
      getOrThrow(props, "SSID")
    )
    Settings(
      getOrThrow(props, "data_dir"),
      googleSecrets
    )
  }

  private def getOrThrow(props: Properties, key: String): String = {
    val value = props.getProperty(key)
    if (value == null) {
      throw new IllegalArgumentException("Missing '" + key + "' in " + fileName)
    }
    value
  }
}
