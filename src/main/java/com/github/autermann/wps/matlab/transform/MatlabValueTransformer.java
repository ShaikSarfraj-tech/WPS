/*
 * Copyright (C) 2013 Christian Autermann
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.github.autermann.wps.matlab.transform;

import java.io.IOException;
import java.util.List;

import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.bbox.BoundingBoxData;
import org.n52.wps.server.ExceptionReport;

import com.github.autermann.matlab.value.MatlabArray;
import com.github.autermann.matlab.value.MatlabCell;
import com.github.autermann.matlab.value.MatlabFile;
import com.github.autermann.matlab.value.MatlabMatrix;
import com.github.autermann.matlab.value.MatlabString;
import com.github.autermann.matlab.value.MatlabStruct;
import com.github.autermann.matlab.value.MatlabValue;
import com.github.autermann.wps.matlab.MatlabFileBinding;
import com.github.autermann.wps.matlab.description.MatlabBoundingBoxOutputDescription;
import com.github.autermann.wps.matlab.description.MatlabComplexOutputDescription;
import com.github.autermann.wps.matlab.description.MatlabInputDescripton;
import com.github.autermann.wps.matlab.description.MatlabLiteralInputDescription;
import com.github.autermann.wps.matlab.description.MatlabLiteralOutputDescription;
import com.github.autermann.wps.matlab.description.MatlabOutputDescription;
import com.google.common.collect.Lists;

public class MatlabValueTransformer {

    public MatlabValue transform(MatlabInputDescripton desc,
                                 List<? extends IData> data) throws
            ExceptionReport {
        if (desc == null) {
            throw new ExceptionReport("No input defintion",
                                      ExceptionReport.NO_APPLICABLE_CODE);
        }
        if (data == null) {
            throw new ExceptionReport(String
                    .format("No data to convert for input %s", desc.getId()),
                                      ExceptionReport.NO_APPLICABLE_CODE);
        }
        if (data.size() < desc.getMinOccurs() ||
            data.size() > desc.getMaxOccurs()) {
            throw new ExceptionReport(String
                    .format("Invalid occurence of input %s", desc.getId()),
                                      ExceptionReport.INVALID_PARAMETER_VALUE);
        }
        if (data.isEmpty()) {
            return null;
        } else if (data.size() == 1) {
            return transformSingleInput(desc, data.get(0));
        } else {
            MatlabCell cell = transformMultiInput(desc, data);
            if (desc instanceof MatlabLiteralInputDescription &&
                ((MatlabLiteralInputDescription) desc).getType().isNumber()) {

                int i = 0;
                double[] values = new double[data.size()];
                for (MatlabValue v : cell) {
                    values[i++] = v.asScalar().value();
                }
                return new MatlabArray(values);
            } else {
                return cell;
            }
        }
    }

    private MatlabCell transformMultiInput(MatlabInputDescripton definition,
                                           List<? extends IData> data)
            throws ExceptionReport {
        List<MatlabValue> values = Lists.newLinkedList();
        for (IData d : data) {
            values.add(transformSingleInput(definition, d));
        }
        return new MatlabCell(values);
    }

    private MatlabValue transformSingleInput(MatlabInputDescripton definition,
                                             IData data) throws ExceptionReport {
        if (definition instanceof MatlabLiteralInputDescription) {
            MatlabLiteralInputDescription name
                    = (MatlabLiteralInputDescription) definition;
            try {
                return name.getType().getTransformation().transformInput(data);
            } catch (IllegalArgumentException e) {
                throw new ExceptionReport(String
                        .format("Can not convert %s for input %s", data, definition
                                .getId()), ExceptionReport.INVALID_PARAMETER_VALUE, e);
            }
        } else if (data instanceof BoundingBoxData) {
            BoundingBoxData boundingBoxData = (BoundingBoxData) data;
            MatlabStruct val = new MatlabStruct();
            val.set("crs", new MatlabString(boundingBoxData.getCRS()));
            val.set("bbox", new MatlabMatrix(new double[][] {
                boundingBoxData.getLowerCorner(),
                boundingBoxData.getUpperCorner()
            }));
            return val;
        } else if (data instanceof MatlabFileBinding) {
            return new MatlabFile(((MatlabFileBinding) data).getPayload());
        } else {
            throw new ExceptionReport("Can not convert input " + data,
                                      ExceptionReport.INVALID_PARAMETER_VALUE);
        }
    }

    public IData transform(MatlabOutputDescription definition,
                           MatlabValue value)
            throws ExceptionReport {
        if (definition == null) {
            throw new ExceptionReport("No output defintion",
                                      ExceptionReport.NO_APPLICABLE_CODE);
        }

        if (value == null) {
            throw new ExceptionReport(String
                    .format("No data to convert for output %s",
                            definition.getId()),
                                      ExceptionReport.NO_APPLICABLE_CODE);
        }

        if (definition instanceof MatlabComplexOutputDescription) {
            return transformComplexOutput(definition, value);
        } else if (definition instanceof MatlabBoundingBoxOutputDescription) {
            return transformBoundingBoxOutput(definition, value);
        } else {
            return transformLiteralOutput(definition, value);
        }
    }

    private IData transformLiteralOutput(MatlabOutputDescription definition,
                                         MatlabValue value)
            throws ExceptionReport {
        try {
            MatlabLiteralOutputDescription literalDefinition
                    = (MatlabLiteralOutputDescription) definition;
            return literalDefinition.getType()
                    .getTransformation()
                    .transformOutput(value);
        } catch (IllegalArgumentException e) {
            throw new ExceptionReport(String
                    .format("Can not convert %s for output %s", value, definition
                            .getId()), ExceptionReport.NO_APPLICABLE_CODE, e);
        }
    }

    private IData transformBoundingBoxOutput(MatlabOutputDescription definition,
                                             MatlabValue value)
            throws ExceptionReport {
        if (value.isMatrix()) {
            String crs = value.asStruct().get("crs").asString().value();
            double[][] bbox = value.asStruct().get("bbox").asMatrix().value();
            return new BoundingBoxData(bbox[0], bbox[1], crs);
        } else {
            throw new ExceptionReport(String
                    .format("Can not convert %s for output %s", value, definition
                            .getId()), ExceptionReport.NO_APPLICABLE_CODE);
        }
    }

    private IData transformComplexOutput(MatlabOutputDescription definition,
                                         MatlabValue value)
            throws ExceptionReport {
        MatlabComplexOutputDescription complexDefinition
                = (MatlabComplexOutputDescription) definition;
        if (value.isFile()) {
            try {
                return new MatlabFileBinding(value.asFile().getContent(),
                                             complexDefinition.getMimeType(),
                                             complexDefinition.getSchema());
            } catch (IOException ex) {
                throw new ExceptionReport(String.format(
                        "Error loading file for output %s", definition
                        .getId()), ExceptionReport.NO_APPLICABLE_CODE);
            }
        } else {
            throw new ExceptionReport(String
                    .format("Can not convert %s for output %s", value, definition
                            .getId()), ExceptionReport.NO_APPLICABLE_CODE);
        }
    }
}
