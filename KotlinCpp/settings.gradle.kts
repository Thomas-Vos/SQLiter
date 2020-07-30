
/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile

enableFeaturePreview("GRADLE_METADATA")
include(":knarch")

//There are dependency issues with native, so we're back to local-only builds for a bit
var konanPath: String? = null//"/Users/kgalligan/temp/kotlin-native-master-hold"
//String konanPath = "C:\\Users\\kgalligan\\devel\\kotlin-native"

if (konanPath == null) {
    val buildDir = File(rootProject.getProjectDir(), "build")
    val distFolder = File(buildDir, "kndist")

    if (!distFolder.exists()) {
        val zipFile = File(buildDir, "kndist.zip")
        val unzipFolder = File(buildDir, "distextract")
        val konanVersion = settings.extra["konanVersion"].toString()
        val kotlinNativeArchive = "https://github.com/JetBrains/kotlin-native/archive/v${konanVersion}.zip"
        downloadKotlinNativeDistro(kotlinNativeArchive, zipFile)
        unzipFileToFolder(zipFile, unzipFolder)
        distroPath(unzipFolder).renameTo(distFolder)
    }

    konanPath = distFolder.path
}

include(":dependencies")
project(":dependencies").projectDir = File(File(konanPath), "dependencies")

includeBuild("${konanPath}/build-tools")
includeBuild("${konanPath}/shared")
includeBuild("${konanPath}/tools/kotlin-native-gradle-plugin")

fun downloadKotlinNativeDistro(path: String, dstFile: java.io.File){
    val url = java.net.URL(path)
    val stream = url.openStream()
    Files.copy(stream, dstFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

}

fun distroPath(unzipFolder: File): File {
    val folders: Array<File> = unzipFolder
            .listFiles { pathname -> pathname?.name?.contains("kotlin-native") ?: false }

    java.util.Arrays.sort(folders)

    return folders.last()
}

fun unzipFileToFolder(zipFile: File, extractFolder: File){
    ZipFile(zipFile).use { zip ->
        zip.entries().asSequence().forEach { entry ->
            zip.getInputStream(entry).use { input ->
                File(extractFolder,entry.name).outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}
