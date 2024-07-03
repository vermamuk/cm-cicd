import groovy.text.GStringTemplateEngine
import hudson.tasks.Mailer
import hudson.tasks.junit.TestResultAction
import hudson.model.User;

def call(body) {
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  //def webhookId = options.webhookId
  def notificationContext = resolveNotificationContext()
  def emailId = config.emailId ?: 'NoReply@oooglobal.com'
  println "Email Distribution List: ${emailId}"
  
  def subject = "BUILD ${env.JOB_NAME} ${notificationContext.build.result}: ${notificationContext.build.id}"
  def bodyTemplate = config.notificationTemplate ?: libraryResource('com/ooo/cm/notification.html')

  emailNotification(notificationContext, subject, bodyTemplate, emailId)

  // TODO polish up teamsNotification implementation
  // teamsNotification(notificationContext, subject, webhookId)
}

@NonCPS
def resolveNotificationContext() {
  def causes = currentBuild.rawBuild.causes.collect { cause -> [shortDescription: cause.shortDescription.toString()]}

  def changeSets = currentBuild.rawBuild.changeSets.collectMany { csList -> 
    return csList.collect { cs ->
      def files = cs.affectedFiles.collect { p -> [editType: p.editType.name.toString(), path: p.path.toString()] }
      def revision = cs.metaClass.hasProperty('commitId') ? cs.commitId : cs.metaClass.hasProperty('revision') ? cs.revision : cs.metaClass.hasProperty('changeNumber') ? cs.changeNumber : ""

      return [
        revision: revision.toString(),
        author: cs.author.toString(),
        authorEmail: getUserConfiguredEmail(cs.author),
        msgAnnotated: cs.msgAnnotated.toString(),
        affectedFiles: files
      ]
    }
  }

  def rawArtifacts = currentBuild.rawBuild.artifacts
  def artifacts = rawArtifacts ? rawArtifacts.collect { artifact -> artifact.toString() } : []

  def testResults = resolveTestResults()

  def build = [
    id: currentBuild.id,
    result: currentBuild.result ?: 'SUCCESS',
    url: currentBuild.absoluteUrl,
    duration: currentBuild.rawBuild.durationString.toString(),
    causes: causes,
    changeSets: changeSets,
    artifacts: artifacts,
    testResults: testResults,
    timestamp: new Date(currentBuild.startTimeInMillis).toString()
  ]

  return [
    build: build,
    project: [
      name: "${env.JOB_NAME}"
    ]
  ]
}

@NonCPS
def resolveTestResults() {
  def junitTestResultAction = currentBuild.rawBuild.getAction(TestResultAction.class)
  
  def testCaseResults = []
  if (junitTestResultAction) {
    def failedTestCaseResults = transformTestCaseResults(junitTestResultAction.getFailedTests())
    testCaseResults.addAll(failedTestCaseResults)
    def passedTestCaseResults = transformTestCaseResults(junitTestResultAction.getPassedTests())
    testCaseResults.addAll(passedTestCaseResults)
    def skippedTestCaseResults = transformTestCaseResults(junitTestResultAction.getSkippedTests())
    testCaseResults.addAll(skippedTestCaseResults)
  }

  return testCaseResults
}

@NonCPS
def transformTestCaseResults(testCases) {
  if (!testCases) {
    return []
  } else {
    return testCases.collect { testCase ->
      def passedTestResults = transformTestResults(testCase.getPassedTests())
      def failedTestResults = transformTestResults(testCase.getFailedTests())
      def skippedTestResults = transformTestResults(testCase.getSkippedTests())

      [
        displayName: testCase.displayName,
        name: testCase.name,
        age: testCase.age,
        failCount: testCase.failCount,
        passCount: testCase.passCount,
        skipCount: testCase.skipCount,
        passedTests: passedTestResults,
        failedTests: failedTestResults,
        skippedTests: skippedTestResults
      ]
    }
  }
}

@NonCPS
def transformTestResults(testResults) {
  if (!testResults) {
    return []
  } else {
    return testResults.collect { testResult ->
      [
        fullName: testResult.fullName,
        status: testResult.status.toString(),
        age: testResult.age
      ]
    }
  }
}

@NonCPS
def transform(String template, Map context) {
  return new GStringTemplateEngine().createTemplate(template).make(context).toString()
}

@NonCPS
def getUserConfiguredEmail(User user) {
  String addr = null;
  if(user != null) {
    Mailer.UserProperty mailProperty = user.getProperty(Mailer.UserProperty.class);
    if (mailProperty != null) {
      addr = mailProperty.getAddress();
    }
  }
  return addr;
}

def emailNotification(notificationContext, subject, bodyTemplate, emailId) {
  /*
   * Research of alternative ways to collect the relevant set of emails.  Note that for now we will use the lists in 
   * the emailext plugin in Jenkins, but it would be nice not to have that dependency
   * 
   * provider types: CulpritsRecipientProvider, DevelopersRecipientProvider, FailingTestSuspectsRecipientProvider, 
   * FirstFailingBuildSuspectsRecipientProvider, ListRecipientProvider, RequesterRecipientProvider, UpstreamComitterRecipientProvider
   *
   * def developersRecipients = notificationContext.build.changeSets.collect { changeSet -> changeSet.authorEmail }
   */
  def body = transform(bodyTemplate, notificationContext)
  
  emailext (
    subject: subject,
    body: body,
    to: emailId,
    recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']]    
  )
}

def teamsNotification(subject, body, webhookId) {
  def title = subject
  def text = body
  
  def webhookUrl = 'https://outlook.office.com/webhook/1f480a6a-f524-41d7-a37b-9fcf2e20328b@e3ff91d8-34c8-4b15-a0b4-18910a6ac575/IncomingWebhook/a3937898842e4dee9ebe253ebaaf1b14/9c4637db-075c-419d-ba68-3bb9c19a55d6'
  
  def payload = """\
  {
    "@type": "MessageCard",
    "@context": "http://schema.org/extensions",
    "themeColor": "0076D7",
    "summary": "Larry Bryant created a new task",
    "sections": [{
        "activityTitle": "![TestImage](https://47a92947.ngrok.io/Content/Images/default.png)Larry Bryant created a new task",
        "activitySubtitle": "On Project Tango",
        "activityImage": "https://teamsnodesample.azurewebsites.net/static/img/image5.png",
        "facts": [{
            "name": "Assigned to",
            "value": "Unassigned"
        }, {
            "name": "Due date",
            "value": "Mon May 01 2017 17:07:18 GMT-0700 (Pacific Daylight Time)"
        }, {
            "name": "Status",
            "value": "Not started"
        }],
        "markdown": true
    }],
    "potentialAction": [{
        "@type": "ActionCard",
        "name": "Add a comment",
        "inputs": [{
            "@type": "TextInput",
            "id": "comment",
            "isMultiline": false,
            "title": "Add a comment here for this task"
        }],
        "actions": [{
            "@type": "HttpPOST",
            "name": "Add comment",
            "target": "https://www.google.com"
        }]
    }, {
        "@type": "ActionCard",
        "name": "Set due date",
        "inputs": [{
            "@type": "DateInput",
            "id": "dueDate",
            "title": "Enter a due date for this task"
        }],
        "actions": [{
            "@type": "HttpPOST",
            "name": "Save",
            "target": "https://www.google.com"
        }]
    }, {
        "@type": "ActionCard",
        "name": "Change status",
        "inputs": [{
            "@type": "MultichoiceInput",
            "id": "list",
            "title": "Select a status",
            "isMultiSelect": "false",
            "choices": [{
                "display": "In Progress",
                "value": "1"
            }, {
                "display": "Active",
                "value": "2"
            }, {
                "display": "Closed",
                "value": "3"
            }]
        }],
        "actions": [{
            "@type": "HttpPOST",
            "name": "Save",
            "target": "https://www.google.com"
        }]
    }]
  }
  """.replaceAll("\n", "")

  sh "curl -H 'Content-Type: text/json; charset=utf-8' -d '${payload}' ${webhookUrl}"
}
