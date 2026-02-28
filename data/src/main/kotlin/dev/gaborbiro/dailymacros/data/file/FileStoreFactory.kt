package dev.gaborbiro.dailymacros.data.file

internal interface FileStoreFactory {

    /**
     * @param folder In order to keep things organised, a non-blank parent folder must be specified
     * for all FileStores. Technically the same one can be re-used, but be mindful that file-name
     * collisions cause override. You must to add this folder to file_paths.xml, even if only used
     * internally, because every FileStore call uses FileProvider.getUriForFile which will complain
     * if the folder is not declared.
     *
     * @param keepFiles If false, the app's private cache folder is used. This folder is wiped when
     * the user does a Clear Data. Furthermore, any file might be deleted by the OS in a low storage
     * situation. If set to true, the app's private /files folder is used where files can live
     * permanently. It is the responsibility of the user of this interface to clean up such files.
     * See [FileStore.delete]
     *
     * @throws IllegalArgumentException if [folder] is blank
     */
    fun getStore(
        folder: String,
        keepFiles: Boolean,
    ): FileStore
}
