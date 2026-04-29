import SwiftUI
import Shared

@main
struct DailyMacrosIOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

struct ContentView: View {
    var body: some View {
        Text(SharedApi.shared.greeting())
            .padding()
    }
}
