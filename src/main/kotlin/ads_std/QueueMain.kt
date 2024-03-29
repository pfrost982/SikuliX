package ads_std

import discord.newDiscord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.sikuli.script.FindFailed
import org.sikuli.script.ImagePath
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

const val LINES = 1
const val ROWS = 1
const val WIDTH = 1200
const val HEIGHT = 900
const val ADDITIONAL_WIDTH = 500

const val CLOSE_PROFILE = false
const val CLOSE_PROFILE_IF_ERROR = false
const val WRITE_TO_ERROR_FILE = false

const val ADD_RANGE = true
const val START_PROFILE = 186
const val END_PROFILE = 190
const val DIRECT_ORDER = false

val error_file = File("src/main/kotlin/ads_std/error_profiles.txt")
val errorList = ConcurrentLinkedQueue<Int>()
suspend fun main() = coroutineScope {
    ImagePath.add("src/main/kotlin/ads_std/png")
    val profiles = mutableListOf<Int>()
    if (ADD_RANGE) {
        if (DIRECT_ORDER) {
            profiles.addAll(START_PROFILE..END_PROFILE)
        } else {
            for (profile in END_PROFILE downTo START_PROFILE) {
                profiles.add(profile)
            }
        }
    }
    println("Profiles:\n$profiles")
    val workRegions = formWorkingRegions(
        LINES, ROWS, WIDTH, HEIGHT,
        screenAdditionalWidth = ADDITIONAL_WIDTH
    )
    val freeWorkRegionsChanel = Channel<WorkRegion>()
    launch { workRegions.forEach { freeWorkRegionsChanel.send(it) } }
    for (profile in profiles) {
        val region = freeWorkRegionsChanel.receive().apply { this.profile = profile }
        launch(Dispatchers.Default) {
            queueOpenProfile(region)
            script(region)
            if (region.profile !in errorList) {
                if (CLOSE_PROFILE) {
                    queueCloseProfile(region)
                }
            } else {
                if (WRITE_TO_ERROR_FILE) {
                    fileAppendString(error_file, "${region.profile}")
                }
                if (CLOSE_PROFILE_IF_ERROR) {
                    queueCloseProfile(region)
                }
            }
            println(foregroundRed + "Error list:\n$errorList" + foregroundDefault)
            println("Work queue:\n$workQueue")
            if (profile == profiles.last()) {
                freeWorkRegionsChanel.close()
                println("Chanel closed...")
            } else {
                freeWorkRegionsChanel.send(region)
            }
        }
        delay(1000)
    }
}

suspend fun script(workRegion: WorkRegion) {
    val screen = workRegion.screen
    workRegion.println(
        "Start, line: ${workRegion.line}, row: ${workRegion.row}", foregroundGreen
    )
    try {
        newDiscord(workRegion)
    } catch (e: FindFailed) {
        workRegion.println("Error", foregroundRed)
        e.printStackTrace()
        workQueue.remove(screen)
        errorList.add(workRegion.profile)
    }
    if (workRegion.profile in errorList) {
        workRegion.println(
            "Finish with error, line: ${workRegion.line}, row: ${workRegion.row}", foregroundRed
        )
    } else {
        workRegion.println(
            "Finish, line: ${workRegion.line}, row: ${workRegion.row}", foregroundGreen
        )
    }
}
