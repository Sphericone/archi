/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package com.archimatetool.editor.diagram.actions;

import java.util.List;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.ui.IWorkbenchPart;

import com.archimatetool.editor.diagram.commands.LineColorCommand;
import com.archimatetool.editor.preferences.IPreferenceConstants;
import com.archimatetool.editor.preferences.Preferences;
import com.archimatetool.editor.ui.ColorFactory;
import com.archimatetool.model.IArchimatePackage;
import com.archimatetool.model.IDiagramModelComponent;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.ILineObject;
import com.archimatetool.model.ILockable;



/**
 * Line Color Action
 * 
 * @author Phillip Beauvoir
 */
public class LineColorAction extends SelectionAction {
    
    public static final String ID = "LineColorAction"; //$NON-NLS-1$
    public static final String TEXT = Messages.LineColorAction_0;
    
    public LineColorAction(IWorkbenchPart part) {
        super(part);
        setText(TEXT);
        setId(ID);
    }

    @Override
    protected boolean calculateEnabled() {
        return getFirstSelectedEditPart(getSelectedObjects()) != null;
    }

    private EditPart getFirstSelectedEditPart(List<?> selection) {
        for(Object object : getSelectedObjects()) {
            if(object instanceof EditPart) {
                Object model = ((EditPart)object).getModel();
                if(model instanceof ILockable && ((ILockable)model).isLocked()) {
                    continue;
                }
                
                if(shouldLineColor(model)) {
                    return (EditPart)object;
                }
            }
        }
        
        return null;
    }
    
    @Override
    public void run() {
        List<?> selection = getSelectedObjects();
        
        ColorDialog colorDialog = new ColorDialog(getWorkbenchPart().getSite().getShell());
        
        // Set default RGB on first selected object
        RGB defaultRGB = null;
        EditPart firstPart = getFirstSelectedEditPart(selection);
        if(firstPart != null) {
            Object model = firstPart.getModel();
            if(shouldLineColor(model)) {
                String s = ((ILineObject)model).getLineColor();
                if(s != null) {
                    defaultRGB = ColorFactory.convertStringToRGB(s);
                }
            }
        }
        
        if(defaultRGB != null) {
            colorDialog.setRGB(defaultRGB);
        }
        else {
            colorDialog.setRGB(new RGB(0, 0, 0));
        }
        
        RGB newColor = colorDialog.open();
        if(newColor != null) {
            execute(createCommand(selection, newColor));
        }
    }
    
    private Command createCommand(List<?> selection, RGB newColor) {
        CompoundCommand result = new CompoundCommand(Messages.LineColorAction_1);
        
        for(Object object : selection) {
            if(object instanceof EditPart) {
                EditPart editPart = (EditPart)object;
                Object model = editPart.getModel();
                
                if(model instanceof ILockable && ((ILockable)model).isLocked()) {
                    continue;
                }
                
                if(shouldLineColor(model)) {
                    Command cmd = new LineColorCommand((ILineObject)model, ColorFactory.convertRGBToString(newColor));
                    if(cmd.canExecute()) {
                        result.add(cmd);
                    }
                }
            }
        }

        return result.unwrap();
    }
    
    private boolean shouldLineColor(Object model) {
        if(model instanceof IDiagramModelObject) {
            // Disable if line colours are derived from fill colours as set in Prefs
            if(Preferences.STORE.getBoolean(IPreferenceConstants.DERIVE_ELEMENT_LINE_COLOR)) {
                return false;
            }
        }
        
        return (model instanceof IDiagramModelComponent) &&
                (((IDiagramModelComponent)model).shouldExposeFeature(IArchimatePackage.Literals.LINE_OBJECT__LINE_COLOR));
    }

}
