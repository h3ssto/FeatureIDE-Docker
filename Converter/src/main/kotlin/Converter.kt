import de.ovgu.featureide.fm.core.base.IFeatureModel
import de.ovgu.featureide.fm.core.init.FMCoreLibrary
import de.ovgu.featureide.fm.core.init.LibraryManager
import de.ovgu.featureide.fm.core.io.IPersistentFormat
import de.ovgu.featureide.fm.core.io.dimacs.DIMACSFormat
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager
import de.ovgu.featureide.fm.core.io.sxfm.SXFMFormat
import de.ovgu.featureide.fm.core.io.uvl.UVLFeatureModelFormat
import de.ovgu.featureide.fm.core.io.xml.XmlFeatureModelFormat
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

object Converter {
    init {
        LibraryManager.registerLibrary(FMCoreLibrary.getInstance())
    }

    private val FormatMap = mapOf(
        Pair(FormatType.DIMACS, "DIMACS"),
        Pair(FormatType.UVL, "UVL"),
        Pair(FormatType.FEATURE_IDE, "FeatureIDE"),
        Pair(FormatType.SXFM, "SXFM"),
    )

    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {

        if (args.isEmpty()) {
            println("Needs at least one input.")
            exitProcess(-1)
        }

        if (args.contains("--help")) {
            println("TODO")
            exitProcess(0)
        }

        val dimacs = args.contains("--dimacs") || args.contains("--all")
        val uvl = args.contains("--uvl") || args.contains("--all")
        val featureIde = args.contains("--featureIde") || args.contains("--all")
        val sxfm = args.contains("--sxfm") || args.contains("--all")

        val check = args.contains("--check")

        val pathFirstRound = "files/first"
        val pathSecondRound = "files/second"

        File(pathFirstRound).deleteRecursively()
        File(pathSecondRound).deleteRecursively()

        Files.createDirectories(Paths.get(pathFirstRound))

        if (check) {
            Files.createDirectories(Paths.get(pathSecondRound))
        }

        val inputFiles = File("files/input").listFiles()

        inputFiles?.let { files ->
            for (file in files) {
                if (file.isDirectory) {
                    continue
                }

                val formatType = getFormatType(file)
                if (formatType == null) {
                    println("Format of file ${file.name} not recognised")
                    continue
                }

                Files.createDirectories(Paths.get("$pathFirstRound/${file.nameWithoutExtension}"))

                val model = FeatureModelManager.load(Paths.get(file.path))
                val formats: MutableList<IPersistentFormat<IFeatureModel>> = mutableListOf()

                if (dimacs) {
                    formats.add(DIMACSFormat())
                }

                if (uvl) {
                    formats.add(UVLFeatureModelFormat())
                }

                if (featureIde) {
                    formats.add(XmlFeatureModelFormat())
                }

                if (sxfm) {
                    formats.add(SXFMFormat())
                }

                for (format in formats) {
                    println("Converting ${file.name} to ${format.suffix}")
                    saveFeatureModel(
                        model,
                        "$pathFirstRound/${file.nameWithoutExtension}${File.separator}${file.nameWithoutExtension}_${FormatMap[formatType]}_${format.name}.${format.suffix}",
                        format,
                    )
                }

                if (check) {

                    println("Running check")
                    Files.createDirectories(Paths.get("$pathSecondRound/${file.nameWithoutExtension}"))

                    val formatFile = when (formatType) {
                        FormatType.DIMACS -> DIMACSFormat()
                        FormatType.UVL -> UVLFeatureModelFormat()
                        FormatType.FEATURE_IDE -> XmlFeatureModelFormat()
                        FormatType.SXFM -> SXFMFormat()
                        else -> {
                            println("Format not found")
                            exitProcess(-1)
                        }
                    }

                    val checksum = file.readBytes().contentHashCode()

                    val folderContent = File("$pathFirstRound/${file.nameWithoutExtension}").listFiles()
                    folderContent?.let { converted ->
                        for (fileEntry in converted){
                            if (fileEntry.isDirectory) {
                                continue
                            }

                            if (fileEntry.nameWithoutExtension.contains("${file.name}_${FormatMap[formatType]}")) {
                                continue
                            }

                            val fileNameSecondRound = "$pathSecondRound/${file.nameWithoutExtension}${File.separator}${fileEntry.nameWithoutExtension}_${formatFile.name}.${formatFile.suffix}"

                            saveFeatureModel(
                                model,
                                fileNameSecondRound,
                                formatFile,
                            )

                            val fileSecondRound = File(fileNameSecondRound)

                            val sum = fileSecondRound.readBytes().contentHashCode()

                            if (sum == checksum) {
                                println("success $fileNameSecondRound")
                            } else {
                                println("failure $fileNameSecondRound")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun saveFeatureModel(model: IFeatureModel?, savePath: String, format: IPersistentFormat<IFeatureModel>?) {
        FeatureModelManager.save(model, Paths.get(savePath), format)
    }

    private fun getFormatType(file: File): Int? {
        return when (file.extension) {
            "dimacs" -> FormatType.DIMACS
            "uvl" -> FormatType.UVL
            "xml" -> {
                var result: Int? = null
                file.bufferedReader().use {
                    for (line in it.lines()) {
                        with(line) {
                            when {
                                contains("<featureModel>") -> result = FormatType.FEATURE_IDE
                                contains("<feature_model") -> result = FormatType.SXFM
                            }
                        }
                        if (result != null) {
                            break
                        }
                    }
                }
                result
            }
            else -> null
        }
    }
}