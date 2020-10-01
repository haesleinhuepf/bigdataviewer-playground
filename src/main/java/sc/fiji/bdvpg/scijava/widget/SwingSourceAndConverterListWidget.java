package sc.fiji.bdvpg.scijava.widget;

import bdv.viewer.SourceAndConverter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.swing.widget.SwingInputWidget;
import org.scijava.widget.InputWidget;
import org.scijava.widget.WidgetModel;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.scijava.services.ui.RenamableSourceAndConverter;
import sc.fiji.bdvpg.scijava.services.ui.SourceAndConverterTreeCellRenderer;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Swing implementation of {@link SourceAndConverterListWidget}.
 *
 * Note the rather complex {@link SwingSourceAndConverterListWidget#set} method to avoid memory leak
 *
 * @author Nicolas Chiaruttini
 */

@Plugin(type = InputWidget.class)
public class SwingSourceAndConverterListWidget extends SwingInputWidget<SourceAndConverter[]> implements
        SourceAndConverterListWidget<JPanel> {

    @Override
    protected void doRefresh() {
    }

    @Override
    public boolean supports(final WidgetModel model) {
        return super.supports(model) && model.isType(SourceAndConverter[].class);
    }

    @Override
    public SourceAndConverter[] getValue() {
        return getSelectedSourceAndConverters();
    }

    @Parameter
	SourceAndConverterService bss;

    public SourceAndConverter[] getSelectedSourceAndConverters() {
        Set<SourceAndConverter> sacList = new HashSet<>(); // A set avoids duplicate SourceAndConverter
        for (TreePath tp : tree.getSelectionModel().getSelectionPaths()) {
            if (((DefaultMutableTreeNode) tp.getLastPathComponent()).getUserObject() instanceof RenamableSourceAndConverter) {
                Object userObj = ((RenamableSourceAndConverter) ((DefaultMutableTreeNode) tp.getLastPathComponent()).getUserObject()).sac;
                sacList.add((SourceAndConverter) userObj);
            } else {
                sacList.addAll(getSourceAndConvertersFromChildrenOf((DefaultMutableTreeNode) tp.getLastPathComponent()));
            }
        }
        return sacList.toArray(new SourceAndConverter[sacList.size()]);
    }

    private Set<SourceAndConverter> getSourceAndConvertersFromChildrenOf(DefaultMutableTreeNode node) {
        Set<SourceAndConverter> sacs = new HashSet<>();
        for (int i=0;i<node.getChildCount();i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            if (child.getUserObject() instanceof RenamableSourceAndConverter) {
                Object userObj = ((RenamableSourceAndConverter) (child.getUserObject())).sac;
                sacs.add((SourceAndConverter) userObj);
            } else {
                sacs.addAll(getSourceAndConvertersFromChildrenOf(child));
            }
        }
        return sacs;
    }

    JTree tree;

    @Override
    public void set(final WidgetModel model) {
        super.set(model);
        tree = new JTree(bss.getUI().getTreeModel());
        tree.setCellRenderer(new SourceAndConverterTreeCellRenderer());

        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(350, 200));
        getComponent().add(scrollPane);
        refreshWidget();
        model.setValue(null);
        TreeSelectionListener tsl = (e)-> model.setValue(getValue());
        tree.addTreeSelectionListener(tsl); // Memory leak... How ot solve this ?

        // -------------------------------- Memory leak! Cut heads of the Hydra of Lerna
        // The part below helps solve the memory leak:
        // with JTree not released the lastly selected path
        // with Listeners holding references with objects of potentially big memory footprint (SourceAndConverters)
        // Maybe related:
        // https://bugs.openjdk.java.net/browse/JDK-6472844
        // https://stackoverflow.com/questions/4517931/java-swing-jtree-is-not-garbage-collected
        // this one more particularly :


        tree.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
                tree.removeTreeSelectionListener(tsl);
                tree.clearSelection();
                tree.cancelEditing();
                //tree.clearToggledPaths();
                tree.resetKeyboardActions();
                tree.updateUI();
                scrollPane.remove(tree);
                getComponent().remove(scrollPane);
                tree.setModel(null);
                tree.removeAncestorListener(this);
                tree = null;
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
            }
        });
        // -------------------------------- All heads cut (hopefully)

    }

}
