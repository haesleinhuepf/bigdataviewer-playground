package sc.fiji.bdvpg.tests;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.script.ScriptService;
import org.scijava.ui.UIService;
import org.scijava.ui.swing.SwingUI;
import sc.fiji.bdvpg.scijava.command.source.NewSourceCommand;
import sc.fiji.bdvpg.scijava.command.source.SourcesRemoverCommand;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.util.concurrent.ExecutionException;

public class TestSourcesCommands {
    Context ctx;

    SourceAndConverterService sourceService;
    SourceAndConverterBdvDisplayService sourceDisplayService;
    CommandService commandService;
    @Before
    public void startFiji() {
        // Initializes static SourceService
        ctx = new Context();
        // Show UI
        ctx.service(UIService.class).showUI(SwingUI.NAME);

        sourceDisplayService = ctx.getService(SourceAndConverterBdvDisplayService.class);
        sourceService = ctx.getService(SourceAndConverterService.class);
        commandService = ctx.getService(CommandService.class);

        // Open two example sources
        new SpimDataFromXmlImporter("src/test/resources/mri-stack.xml").get();
        new SpimDataFromXmlImporter("src/test/resources/demoSlice.xml").get();

    }

    @Test(timeout=5000)
    public void testSourceDeleteCommand() throws ExecutionException, InterruptedException {
        Assert.assertEquals("Error - there should be two sources at the beginning of the test", 2, sourceService.getSourceAndConverters().size() );
        commandService.run(SourcesRemoverCommand.class,true, "sacs", "mri-stack.xml").get();
        Assert.assertEquals("Error - there should be one source left", 1, sourceService.getSourceAndConverters().size());
    }

    @Test(timeout=5000)
    public void testSourceDeleteIJ1Macro() throws ExecutionException, InterruptedException{
        ctx.getService(ScriptService.class).run("dummy.ijm",
                "run(\"Delete Sources\", \"sacs=[mri-stack.xml]\");", true).get();
        Assert.assertEquals("Error - there should be one source left", 1, sourceService.getSourceAndConverters().size() );
    }

    @Test(timeout=5000)
    public void testSourceDeleteNestedPathCommand() throws ExecutionException, InterruptedException {
        Assert.assertEquals("Error - there should be two sources at the beginning of the test", 2, sourceService.getSourceAndConverters().size() );
        commandService.run(SourcesRemoverCommand.class,true, "sacs", "mri-stack.xml>Channel>1").get();
        Assert.assertEquals("Error - there should be one source left", 1, sourceService.getSourceAndConverters().size());
    }

    @Test(timeout=5000)
    public void testSourceDeleteNestedPathIJ1Macro() throws ExecutionException, InterruptedException {
        Assert.assertEquals("Error - there should be two sources at the beginning of the test", 2, sourceService.getSourceAndConverters().size() );
        ctx.getService(ScriptService.class).run("dummy.ijm",
                "run(\"Delete Sources\", \"sacs=[mri-stack.xml>Channel>1]\");", true).get();
        Assert.assertEquals("Error - there should be one source left", 1, sourceService.getSourceAndConverters().size());
    }

    /*
    // Splitting by comma is not supported and could be a bad idea. Let's refrain from it until it becomes absolutely necessary
    The workaround is to run several times the deletion on the different paths manually
    @Test(timeout=5000)
    public void testMultipleSourcesDeleteCommand() throws ExecutionException, InterruptedException {
        Assert.assertEquals("Error - there should be two sources at the beginning of the test", 2, sourceService.getSourceAndConverters().size() );
        commandService.run(SourcesRemoverCommand.class,true, "sacs", "mri-stack.xml, demoSlice.xml").get();
        Assert.assertEquals("Error - there should be one source left", 1, sourceService.getSourceAndConverters().size());
    }

    @Test(timeout=5000)
    public void testMultipleSourcesDeleteIJ1Macro() throws ExecutionException, InterruptedException {
        Assert.assertEquals("Error - there should be two sources at the beginning of the test", 2, sourceService.getSourceAndConverters().size() );
        ctx.getService(ScriptService.class).run("dummy.ijm",
                "run(\"Delete Sources\", \"sacs=[mri-stack.xml, demoSlice.xml]\");", true).get();
        Assert.assertEquals("Error - there should be one source left", 1, sourceService.getSourceAndConverters().size());
    }*/

    //NewSourceCommand
    @Test(timeout=5000)
    public void testNewSourceCommand() throws ExecutionException, InterruptedException {
        Assert.assertEquals("Error - there should be two sources at the beginning of the test", 2, sourceService.getSourceAndConverters().size());
        commandService.run(NewSourceCommand.class,true,
                "model", "mri-stack.xml>Channel>1",
                "name", "model",
                "voxsizex",1,
                "voxsizey",1,
                "voxsizez",1,
                "timepoint",0).get();
        Assert.assertEquals("Error - there should three sources after one is created", 3, sourceService.getSourceAndConverters().size());
    }

    @Test(timeout=5000)
    public void testNewSourceIJ1Macro() throws ExecutionException, InterruptedException {
        Assert.assertEquals("Error - there should be two sources at the beginning of the test", 2, sourceService.getSourceAndConverters().size());
        //New Source Based on Model Source
        ctx.getService(ScriptService.class).run("dummy.ijm",
                "run(\"New Source Based on Model Source\", \""+
                        "model=[mri-stack.xml>Channel>1] " +
                        "name=model " +
                        "voxsizex=1 " +
                        "voxsizey=1 " +
                        "voxsizez=1 " +
                        "timepoint=0 " +
                        "\");", true).get();
        Assert.assertEquals("Error - there should three sources after one is created", 3, sourceService.getSourceAndConverters().size());
    }

    @After
    public void closeFiji() {

        // Closes bdv windows
        sourceDisplayService.getDisplays().forEach(BdvHandle::close);

        // Clears all sources
        sourceService.remove(sourceService.getSourceAndConverters().toArray(new SourceAndConverter[0]));

        // Closes context
        ctx.close();
    }

}
