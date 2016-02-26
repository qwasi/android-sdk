## SDK Version 2.1.19-6 / 2016-02-26

- [FIX] #noissue version bump
- [FIX] should fix [DROID-48]
- [FIX] fixes [DROID-48]
- [FIX] added more options for the text/image/json, and fixed issue where silents where being sent to the notificationbar
- [FIX] location manager modifications
- [FIX] fixes issue where notifications where not being displayed, comments for style guides, and modifing locationmanager
- [FIX] Qwasi fixing line issue and static string being incorrectly used, GCMListener added fetchfailure note since we can build it from the bundle. Reverted QwasiLocationMng service changes.  QwasiService allowed for messages to be built from the bundle as well
- [FIX] fixes [DROID-47]
- [FIX] changing where the default message is sent.
- [FIX] qwasiLocation transistion to service
- [FIX] issue where application was flagged as being closed erronously
- [FIX] made QwasiGCMListener abstract such that any subclass is forced to implement onQwasiMessage, made a static function for the service to send the notification which would involve instating a abstract class as a concrete that try catches already set up for the custom class.
- [FIX] #noissue more android comments/style changes
- [FIX] refactoring, and adding method comments to fit android style
