# User-Interface Quality-Assurance Data-Driven-Tests Plugin


This plugins allows to define your "tests"
(from [https://app.uiqa.io](https://app.uiqa.io/))   
in the global configuration. These tests can then be "set" by jobs as a
build step.   
This job will re-run this test and retrieve the JUnit report.

## Quick usage guide

-   Install the plugin
-   In *Manage Jenkins \> Configure System* go to ***QA DDT***  
    ***  
    -   Set **Username & Password**:
        
        ![](https://wiki.jenkins.io/download/attachments/165580103/QA-DDT-Credentials.png?version=1&modificationDate=1544032403000&api=v2)
        
    -   **Add tests**:
       
        ![](https://wiki.jenkins.io/download/attachments/165580103/QA-DDT-Tests.png?version=1&modificationDate=1544032583000&api=v2)
Brief description of the named fields:

        -   ***UUID***- The UUID of the test you would like to run
            later on from the [QA DDT App](https://app.uiqa.io/)
            (just copy-paste the desired UUID).  
        -   ***Name*** - A name describing the test ( Mandatory! )
        -   ***Tags***- Additional tags for the test (see, ["tags" in
            our official
            docs](https://github.com/freaker2k7/ui-data-driven-tests/blob/master/3-Advanced.md#tags---a-list-of-tags-of-the-task))

&nbsp;

-   In *Jenkins \> {job name} \>* Configure \> ***Build***  
    ***  
    -   Choose a test from the list of tests you set in the Jenkins
        Configuration  
          
        ![](https://wiki.jenkins.io/download/attachments/165580103/QA-DDT-Build.png?version=1&modificationDate=1544033426000&api=v2)

        Brief description of the named fields:

        -   ***Name*** - The name of the test from the list.
        -   ***Tags (Advanced)*** - A list of tags (separated by comas
            without spaces) to override the test's tags for this job.

-   (*Optional*) In *Jenkins \> {job name} \>* Configure \> **Post-build
    Actions**  
      
    -   Add "report\*.xml" to the **Test report XMLs** field in order to
        get the JUnit report of the DDT test.  
          
        ![](https://wiki.jenkins.io/download/attachments/165580103/Screen%20Shot%202019-03-26%20at%208.45.47%20PM.png?version=1&modificationDate=1553625959000&api=v2)

&nbsp;

-   Start a build.  
      
    -   You should see something like this in the **Console Output**:  
          

        ``` console-output
        Started by user anonymous
        Building in workspace /home/jenkins/work/jobs/Test_My_UI/workspace
        Initializing test: bfb408603f48ff882f3eea1a7dc778f6dbcf9b12cb548906e5aafce856c8f4ac()
        Waiting for test '57b61ec71bf53fa35e897b8e8a5aaaf955ed31e7b8e9332e62496c58335b638e' to start
        Testing...
        Done test: '57b61ec71bf53fa35e897b8e8a5aaaf955ed31e7b8e9332e62496c58335b638e'
        Recording test results
        Finished: SUCCESS
        ```

        bfb4086... is the original test UUID from Jenkins'
        Configuration.  
        57b61ec... is a new UUID from this specific run.
***

## **Changelog**

 

### Release 1.0 (2018-11-29)

-   Initial release
