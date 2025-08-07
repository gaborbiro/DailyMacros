package dev.gaborbiro.dailymacros.features.main

interface NavDestination {
    val route: String
}

object NoteList : NavDestination {
    override val route = "notes"
}

//object AddNoteViaCamera : NotesIntentDestination {
//    override val route = "camera"
//    override val intent = HostActivity.Companion::getCameraIntent
//}
//
//object AddNoteViaImage : NotesIntentDestination {
//    override val route = "pick_image"
//    override val intent = HostActivity.Companion::getImagePickerIntent
//}
//
//object AddNoteViaText : NotesIntentDestination {
//    override val route = "text"
//    override val intent = HostActivity.Companion::getTextOnlyIntent
//}
