import de.ovgu.featureide.fm.core.base.IFeature
import de.ovgu.featureide.fm.core.base.IFeatureModel
import de.ovgu.featureide.fm.core.base.event.IEventListener
import de.ovgu.featureide.fm.core.init.FMCoreLibrary
import de.ovgu.featureide.fm.core.init.LibraryManager
import de.ovgu.featureide.fm.core.io.IPersistentFormat
import de.ovgu.featureide.fm.core.io.dimacs.DIMACSFormat
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager
import de.ovgu.featureide.fm.core.io.sxfm.SXFMFormat
import de.ovgu.featureide.fm.core.io.uvl.UVLFeatureModelFormat
import de.ovgu.featureide.fm.core.io.xml.XmlFeatureModelFormat
import de.ovgu.featureide.fm.core.job.SliceFeatureModel
import de.ovgu.featureide.fm.core.job.monitor.NullMonitor
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors
import kotlin.io.path.extension
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
            println("You did not select any command")
            exitProcess(0)
        }

        if (args.contains("--help")) {
            println("This will later display all the information for the different tasks")
            exitProcess(0)
        }

        val dimacs = args.contains("--dimacs") || args.contains("--all")
        val uvl = args.contains("--uvl") || args.contains("--all")
        val featureIde = args.contains("--featureIde") || args.contains("--all")
        val sxfm = args.contains("--sxfm") || args.contains("--all")

        val check = args.contains("--check")

        if (args.contains("--slice")){
            val file = File(args.get(args.indexOf("--slice") + 1))
            val featureNames = args.get(args.indexOf("--slice") + 2).split(",")

            val path = Paths.get(file.path)

            var model = FeatureModelManager.load(path)
            val featuresToSlice = ArrayList<IFeature>()

            for (name in featureNames){
                featuresToSlice.add(model.getFeature(name))
            }

            model = slice(model, featuresToSlice)

            saveFeatureModel(model, path.parent.toString() + "new.xml", XmlFeatureModelFormat())
        }

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

                // Files.createDirectories(Paths.get("$pathFirstRound/${file.nameWithoutExtension}"))

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
                        "$pathFirstRound/${file.nameWithoutExtension}.${format.suffix}",
                        format,
                    )
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

    private fun slice(featureModel: IFeatureModel, featuresToRemove: Collection<IFeature>?): IFeatureModel? {
        val featuresToKeep: MutableSet<IFeature> = HashSet(featureModel.features)
        featuresToKeep.removeAll(featuresToRemove!!.toSet())
        val featureNamesToKeep: Set<String> = featuresToKeep.stream().map { obj: IFeature -> obj.name }.collect(Collectors.toSet())
        val sliceJob = SliceFeatureModel(featureModel, featureNamesToKeep, false)
        return try {
            sliceJob.execute(NullMonitor())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}