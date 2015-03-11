/* Copyright 2015 Wouter Spekkink
Authors : Wouter Spekkink <wouterspekkink@gmail.com>
Website : http://www.wouterspekkink.org
DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
Copyright 2015 Wouter Spekkink. All rights reserved.
The contents of this file are subject to the terms of either the GNU
General Public License Version 3 only ("GPL") or the Common
Development and Distribution License("CDDL") (collectively, the
"License"). You may not use this file except in compliance with the
License. You can obtain a copy of the License at
http://gephi.org/about/legal/license-notice/
or /cddl-1.0.txt and /gpl-3.0.txt. See the License for the
specific language governing permissions and limitations under the
License. When distributing the software, include this License Header
Notice in each file and include the License files at
/cddl-1.0.txt and /gpl-3.0.txt. If applicable, add the following below the
License Header, with the fields enclosed by brackets [] replaced by
your own identifying information:
"Portions Copyrighted [year] [name of copyright owner]"
If you wish your version of this file to be governed by only the CDDL
or only the GPL Version 3, indicate your decision by adding
"[Contributor] elects to include this software in this distribution
under the [CDDL or GPL Version 3] license." If you do not indicate a
single choice of license, a recipient has the option to distribute
your version of this file under either the CDDL, the GPL Version 3 or
to extend the choice of license to its licensees as provided above.
However, if you add GPL Version 3 code and therefore, elected the GPL
Version 3 license, then the option applies only if the new code is
made subject to such option by the copyright holder.
Contributor(s): Wouter Spekkink

The plugin structure was inspired by the structure of the GeoLayout plugin.
*/

package org.wouterspekkink.mdslayout;

import java.util.ArrayList;
import java.util.List;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeRow;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.graph.spi.LayoutData;
import org.gephi.layout.spi.Layout;
import org.gephi.layout.spi.LayoutBuilder;
import org.gephi.layout.spi.LayoutProperty;
import org.gephi.ui.propertyeditor.NodeColumnNumbersEditor;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 *
 * @author wouter
 */

public class MDSLayout implements Layout {
    
    private final MDSLayoutBuilder builder;
    private GraphModel graphModel;
    //Params
    private double scale = 1000;
    private AttributeColumn dimension1;
    private AttributeColumn dimension2;
  

    public MDSLayout(MDSLayoutBuilder builder) {
        this.builder = builder;
        resetPropertiesValues();
    }

    @Override
    public void resetPropertiesValues() {
        AttributeModel attModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        for (AttributeColumn c : attModel.getNodeTable().getColumns()) {
            if (c.getId().equalsIgnoreCase("dimension1")
                    || c.getId().equalsIgnoreCase("dim1")
                    || c.getTitle().equalsIgnoreCase("dimension1")
                    || c.getTitle().equalsIgnoreCase("dim1")) {
                dimension1 = c;
            } else if (c.getId().equalsIgnoreCase("dimension2")
                    || c.getId().equalsIgnoreCase("dim2")
                    || c.getTitle().equalsIgnoreCase("dimension2")
                    || c.getTitle().equalsIgnoreCase("dim2")) {
                dimension2 = c;
            }
        }
    }

    @Override
    public void initAlgo() {
    }
    
     @Override
    public void goAlgo() {
        double dim1 = 0;
        double dim2 = 0;
        float nodeX = 0;
        float nodeY = 0;
        Graph graph = graphModel.getGraph();

        graph.readLock();

        Node[] nodes = graph.getNodes().toArray();
        List<Node> validNodes = new ArrayList<Node>();
        List<Node> unvalidNodes = new ArrayList<Node>();

        // Set valid and non valid nodes:
        for (Node n : nodes) {
            AttributeRow row = (AttributeRow) n.getNodeData().getAttributes();
            if (row.getValue(dimension1) != null && row.getValue(dimension2) != null) {
                validNodes.add(n);
            } else {
                unvalidNodes.add(n);
            }
        }

        for (Node n : validNodes) {
            if (n.getNodeData().getLayoutData() == null || !(n.getNodeData().getLayoutData() instanceof MDSLayoutData)) {
                n.getNodeData().setLayoutData(new MDSLayoutData());
                }
            dim1 = getDoubleValue(n, dimension1);
            dim2 = getDoubleValue(n, dimension2);

            nodeX = (float) (dim1 * scale);
            nodeY = (float) (dim2 * scale);

            n.getNodeData().setX(nodeX);
            n.getNodeData().setY(nodeY);
        }


        if (validNodes.size() > 0 && unvalidNodes.size() > 0) {
            Node tempNode = validNodes.get(0);
            double xMin = tempNode.getNodeData().x();
            double xMax = tempNode.getNodeData().x();
            double yMin = tempNode.getNodeData().y();
            double xTemp = 0;
            double yTemp = 0;

            for (Node n : validNodes) {
                xTemp = n.getNodeData().x();
                yTemp = n.getNodeData().y();

                if (xTemp < xMin) {
                    xMin = xTemp;
                }
                if (xTemp > xMax) {
                    xMax = xTemp;
                }
                if (yTemp < yMin) {
                    yMin = yTemp;
                }
            }

            if (unvalidNodes.size() > 1) {
                double i = 0;
                double step = (xMax - xMin) / (unvalidNodes.size() - 1);
                for (Node n : unvalidNodes) {
                    n.getNodeData().setX((float) (xMin + i * step));
                    n.getNodeData().setY((float) (yMin - step));
                    i++;
                }
            } else {
                tempNode = unvalidNodes.get(0);
                tempNode.getNodeData().setX(10000);
                tempNode.getNodeData().setY(10000);
            }
        }
        graph.readUnlock();
    }
    
    public double getDoubleValue(Node node, AttributeColumn column) {
        return ((Number) node.getNodeData().getAttributes().getValue(column.getIndex())).doubleValue();
        }

    @Override
    public void endAlgo() {
    }

    @Override
    public boolean canAlgo() {
        return dimension1 != null && dimension2 != null;
    }
    
    @Override
    public LayoutProperty[] getProperties() {
        List<LayoutProperty> properties = new ArrayList<LayoutProperty>();
        final String MDSLAYOUT = "MDS Layout";

        try {
            properties.add(LayoutProperty.createProperty(
                    this, Double.class,
                    "Network Scale",
                    MDSLAYOUT,
                    "Determines the scale of the network",
                    "getScale", "setScale"));
            properties.add(LayoutProperty.createProperty(
                    this, AttributeColumn.class,
                    "Dimension 1",
                    MDSLAYOUT,
                    "Choose the first dimension to be used in the layout",
                    "getDimension1", "setDimension1", NodeColumnNumbersEditor.class));
            properties.add(LayoutProperty.createProperty(
                    this, AttributeColumn.class,
                    "Dimension 2",
                    MDSLAYOUT,
                    "Choose the second dimension to be used in the layout",
                    "getDimension2", "setDimension2", NodeColumnNumbersEditor.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return properties.toArray(new LayoutProperty[0]);
    }
    
    @Override
    public void setGraphModel(GraphModel graphModel) {
        this.graphModel = graphModel;
    }

    @Override
    public LayoutBuilder getBuilder() {
        return builder;
    }

    public Double getScale() {
        return scale;
    }

    public void setScale(Double scale) {
        this.scale = scale;
    }

    public AttributeColumn getDimension1() {
        return dimension1;
    }

    public void setDimension1(AttributeColumn dimension1) {
        this.dimension1 = dimension1;
    }

    public AttributeColumn getDimension2() {
        return dimension2;
    }

    public void setDimension2(AttributeColumn dimension2) {
        this.dimension2 = dimension2;
    }

    private static class MDSLayoutData implements LayoutData {

        //Data
        public double x = 0f;
        public double y = 0f;
    }
    
}

