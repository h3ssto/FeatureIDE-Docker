import de.ovgu.featureide.fm.core.base.IFeature
import de.ovgu.featureide.fm.core.base.IFeatureModel
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
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import java.io.File
import java.io.IOException
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.stream.Collectors
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

        val parser = ArgParser("featureide-docker")

        val path by parser.option(ArgType.String, shortName = "p", description = "Input path for file or directory.").required()
        val slice by parser.option(ArgType.String, shortName = "s", description = "The names of the features that should be sliced separated by ','. For example: Antenna,AHEAD.")
        val check by parser.option(ArgType.String, shortName = "c", description = "Input path for the second file that should be checked with the first one.")
        val all by parser.option(ArgType.Boolean, shortName = "a", description = "Parsers all files from path into all formats.").default(false)
        val dimacs by parser.option(ArgType.Boolean, shortName = "d", description = "Parses all files from path into dimacs files.").default(false)
        val uvl by parser.option(ArgType.Boolean, shortName = "u", description = "Parses all files from path into uvl files.").default(false)
        val sxfm by parser.option(ArgType.Boolean, shortName = "sf", description = "Parses all files from path into sxfm(xml) files.").default(false)
        val featureIde by parser.option(ArgType.Boolean, shortName = "fi", description = "Parses all files from path into featureIde(xml) files.").default(false)
        parser.parse(args)


        val file = File(path)

        val output = "./files/output"

        File(output).deleteRecursively()

        Files.createDirectories(Paths.get(output))

        //slices the featureModel
        if (!slice.isNullOrEmpty()){
            if(file.isDirectory() || !file.exists()) exitProcess(0)
            var model = FeatureModelManager.load(Paths.get(file.path))
            val featuresToSlice = ArrayList<IFeature>()

            for (name in slice!!.split(",")){
                featuresToSlice.add(model.getFeature(name))
            }
            model = slice(model, featuresToSlice)
            saveFeatureModel(model, "${output}/${file.nameWithoutExtension}.${XmlFeatureModelFormat().suffix}", XmlFeatureModelFormat())
            exitProcess(0)
        }

        //checks if two featureModel are the same
        if (!check.isNullOrEmpty()){
            val file2 = File(check)
            if(file.isDirectory() || !file.exists() || file2.isDirectory() || !file2.exists()) exitProcess(0)
            var model = FeatureModelManager.load(Paths.get(file.path))
            var model2 = FeatureModelManager.load(Paths.get(file2.path))

            val first = File.createTempFile("temp1", ".xml")
            val second = File.createTempFile("temp2", ".xml")
            saveFeatureModel(model, first.path, XmlFeatureModelFormat())
            saveFeatureModel(model2, second.path, XmlFeatureModelFormat())
            first.deleteOnExit()
            second.deleteOnExit()
            val md = MessageDigest.getInstance("MD5")
            val hash1 = md.digest(first.readBytes())
            val checksum1 = BigInteger(1, hash1).toString(16)
            val hash2 = md.digest(second.readBytes())
            val checksum2 = BigInteger(1, hash2).toString(16)
            System.out.print(checksum1.equals(checksum2))

            exitProcess(0)
        }

        if(file.isDirectory){
            val inputFiles = file.listFiles()
            inputFiles?.let { files ->
                for (fileFromList in files) {
                    if (fileFromList.isDirectory) {
                        continue
                    }

                    val model = FeatureModelManager.load(Paths.get(fileFromList.path))
                    val formats: MutableList<IPersistentFormat<IFeatureModel>> = mutableListOf()

                    if (dimacs || all) {
                        formats.add(DIMACSFormat())
                    }

                    if (uvl || all) {
                        formats.add(UVLFeatureModelFormat())
                    }

                    if (featureIde || all) {
                        formats.add(XmlFeatureModelFormat())
                    }

                    if (sxfm || all) {
                        formats.add(SXFMFormat())
                    }

                    for (format in formats) {
                        println("Converting ${file.name} to ${format.suffix}")
                        saveFeatureModel(
                            model,
                            "${output}/${fileFromList.nameWithoutExtension}.${format.suffix}",
                            format,
                        )
                    }
                }
            }
        } else if (file.exists()){

        }
    }

    private fun saveFeatureModel(model: IFeatureModel?, savePath: String, format: IPersistentFormat<IFeatureModel>?) : String{
        FeatureModelManager.save(model, Paths.get(savePath), format)
        return savePath
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