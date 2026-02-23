import com.android.build.api.dsl.CommonExtension
import com.tishukoff.aiadvent.configureAndroidCompose
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val extension = extensions.getByType<CommonExtension>()
            configureAndroidCompose(extension)
        }
    }
}
