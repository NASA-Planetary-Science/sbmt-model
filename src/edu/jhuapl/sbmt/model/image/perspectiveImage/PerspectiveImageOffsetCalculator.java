package edu.jhuapl.sbmt.model.image.perspectiveImage;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import edu.jhuapl.saavtk.util.MathUtil;

class PerspectiveImageOffsetCalculator
{
    double[][] spacecraftPositionAdjusted = new double[1][3];
    double[][] frustum1Adjusted = new double[1][3];
    double[][] frustum2Adjusted = new double[1][3];
    double[][] frustum3Adjusted = new double[1][3];
    double[][] frustum4Adjusted = new double[1][3];
    double[][] boresightDirectionAdjusted = new double[1][3];
    double[][] upVectorAdjusted = new double[1][3];
    double[][] sunPositionAdjusted = new double[1][3];

    // apply all frame adjustments if true
    boolean[] applyFrameAdjustments = { true };

    // location in pixel coordinates of the target origin for the adjusted frustum
    double[] targetPixelCoordinates = { Double.MAX_VALUE, Double.MAX_VALUE };
    double[] zoomFactor = { 1.0 };

    double[] rotationOffset = { 0.0 };
    double[] pitchOffset = { 0.0 };
    double[] yawOffset = { 0.0 };
    double sampleOffset = 0.0;
    double lineOffset = 0.0;

    PerspectiveImage image;

	public PerspectiveImageOffsetCalculator(PerspectiveImage image)
	{
		this.image = image;
	}

    void setTargetPixelCoordinates(double[] frustumCenterPixel)
    {
        this.targetPixelCoordinates[0] = frustumCenterPixel[0];
        this.targetPixelCoordinates[1] = frustumCenterPixel[1];
        setApplyFrameAdjustments(true);
    }

    void setLineOffset(double offset)
    {
        lineOffset = offset;
        setApplyFrameAdjustments(true);
    }

    void setSampleOffset(double offset)
    {
        sampleOffset = offset;
        setApplyFrameAdjustments(true);
    }

    void setRotationOffset(double offset)
    {
        if (rotationOffset == null)
            rotationOffset = new double[1];

        rotationOffset[0] = offset;
        setApplyFrameAdjustments(true);
    }

    void setYawOffset(double offset)
    {
        if (yawOffset == null)
            yawOffset = new double[1];

        yawOffset[0] = offset;
        setApplyFrameAdjustments(true);
    }

    void setPitchOffset(double offset)
    {
        if (pitchOffset == null)
            pitchOffset = new double[1];

        pitchOffset[0] = offset;
        setApplyFrameAdjustments(true);
    }

    void setZoomFactor(double offset)
    {
        if (zoomFactor == null)
            zoomFactor = new double[1];

        zoomFactor[0] = offset;
        setApplyFrameAdjustments(true);
    }

    void setApplyFrameAdjustments(boolean state)
    {
        applyFrameAdjustments[0] = state;
        updateFrameAdjustments();
        image.loadFootprint();
        image.calculateFrustum();
        image.saveImageInfo();
    }

    boolean getApplyFramedAdjustments()
    {
        return applyFrameAdjustments[0];
    }

    void copySpacecraftState()
    {
        int nslices = image.getImageDepth();
        for (int i = 0; i < nslices; i++)
        {
        	spacecraftPositionAdjusted = MathUtil.copy(image.getSpacecraftPositionOriginal());
        	frustum1Adjusted = MathUtil.copy(image.getFrustum1Original());
        	frustum2Adjusted = MathUtil.copy(image.getFrustum2Original());
        	frustum3Adjusted = MathUtil.copy(image.getFrustum3Original());
        	frustum4Adjusted = MathUtil.copy(image.getFrustum4Original());
        	boresightDirectionAdjusted = MathUtil.copy(image.getBoresightDirectionOriginal());
        	upVectorAdjusted = MathUtil.copy(image.getUpVectorOriginal());
        	sunPositionAdjusted = MathUtil.copy(image.getSunPositionOriginal());
        }
    }

    void updateFrameAdjustments()
    {
        // adjust wrt the original spacecraft pointing direction, not the previous
        // adjusted one
        copySpacecraftState();

        if (applyFrameAdjustments[0])
        {
            if (targetPixelCoordinates[0] != Double.MAX_VALUE && targetPixelCoordinates[1] != Double.MAX_VALUE)
            {
                int height = image.getImageHeight();
                double line = height - 1 - targetPixelCoordinates[0];
                double sample = targetPixelCoordinates[1];

                double[] newTargetPixelDirection = image.getPixelDirection(sample, line);
                rotateTargetPixelDirectionToLocalOrigin(newTargetPixelDirection);
            }

            if (sampleOffset != 0 || lineOffset != 0)
            	translateSpacecraftInImagePlane(sampleOffset, lineOffset);
            else
            	translateSpacecraftInImagePlane(0, 0);

            rotateFrameAboutTarget(rotationOffset[0]);
            zoomFrame(zoomFactor[0]);

            image.setUseDefaultFootprint(false);
        }

        int nslices = image.getImageDepth();
        for (int slice = 0; slice < nslices; slice++)
        {
        	resetFrustaAndFootprint(slice);
        }
    }

    void zoomFrame(double zoomFactor)
    {
    	if (zoomFactor == 1.0) return;
        double zoomRatio = 1.0 / zoomFactor;
        int nslices = image.getImageDepth();
        int currentSlice = image.getCurrentSlice();
        for (int slice = 0; slice < nslices; slice++)
        {
            double[][] surfacePoint = new double[nslices][3];

            for (int i = 0; i < 3; i++)
            {
            	surfacePoint[currentSlice][i] = image.getSpacecraftPositionOriginal()[currentSlice][i] + image.getBoresightDirectionOriginal()[currentSlice][i];
            	spacecraftPositionAdjusted[currentSlice][i] = surfacePoint[currentSlice][i] - image.getBoresightDirectionOriginal()[currentSlice][i] * zoomRatio;
            }
            resetFrustaAndFootprint(slice);
        }
    }

    void rotateFrameAboutPitchAxis(double angleDegrees)
    {
    	int nslices = image.getImageDepth();
        for (int slice = 0; slice < nslices; slice++)
        {
        	double[] vout = new double[] { 0.0, 0.0, 0.0 };
        	MathUtil.vsub(frustum1Adjusted[slice], frustum2Adjusted[slice], vout);
        	MathUtil.unorm(vout, vout);
        	Rotation rotation = new Rotation(new Vector3D(vout), Math.toRadians(angleDegrees), RotationConvention.VECTOR_OPERATOR);
        	MathUtil.rotateVector(frustum1Adjusted[slice], rotation, frustum1Adjusted[slice]);
            MathUtil.rotateVector(frustum2Adjusted[slice], rotation, frustum2Adjusted[slice]);
            MathUtil.rotateVector(frustum3Adjusted[slice], rotation, frustum3Adjusted[slice]);
            MathUtil.rotateVector(frustum4Adjusted[slice], rotation, frustum4Adjusted[slice]);
            MathUtil.rotateVector(boresightDirectionAdjusted[slice], rotation, boresightDirectionAdjusted[slice]);

            resetFrustaAndFootprint(slice);
        }
    }

    void rotateFrameAboutYawAxis(double angleDegrees)
    {
    	int nslices = image.getImageDepth();
        for (int slice = 0; slice < nslices; slice++)
        {
        	double[] vout = new double[] { 0.0, 0.0, 0.0 };
        	MathUtil.vsub(frustum1Adjusted[slice], frustum3Adjusted[slice], vout);
        	MathUtil.unorm(vout, vout);
        	Rotation rotation = new Rotation(new Vector3D(vout), Math.toRadians(angleDegrees), RotationConvention.VECTOR_OPERATOR);
        	MathUtil.rotateVector(frustum1Adjusted[slice], rotation, frustum1Adjusted[slice]);
            MathUtil.rotateVector(frustum2Adjusted[slice], rotation, frustum2Adjusted[slice]);
            MathUtil.rotateVector(frustum3Adjusted[slice], rotation, frustum3Adjusted[slice]);
            MathUtil.rotateVector(frustum4Adjusted[slice], rotation, frustum4Adjusted[slice]);
            MathUtil.rotateVector(boresightDirectionAdjusted[slice], rotation, boresightDirectionAdjusted[slice]);

            resetFrustaAndFootprint(slice);
        }
    }

    void rotateFrameAboutTarget(double angleDegrees)
    {
    	if (angleDegrees == 0) return;
        Vector3D axis = new Vector3D(image.getBoresightDirectionOriginal()[image.currentSlice]);
        Rotation rotation = new Rotation(axis, Math.toRadians(angleDegrees), RotationConvention.VECTOR_OPERATOR);

        int nslices = image.getImageDepth();
        for (int slice = 0; slice < nslices; slice++)
        {
            MathUtil.rotateVector(frustum1Adjusted[slice], rotation, frustum1Adjusted[slice]);
            MathUtil.rotateVector(frustum2Adjusted[slice], rotation, frustum2Adjusted[slice]);
            MathUtil.rotateVector(frustum3Adjusted[slice], rotation, frustum3Adjusted[slice]);
            MathUtil.rotateVector(frustum4Adjusted[slice], rotation, frustum4Adjusted[slice]);
            MathUtil.rotateVector(boresightDirectionAdjusted[slice], rotation, boresightDirectionAdjusted[slice]);

            resetFrustaAndFootprint(slice);
        }
    }

    void translateSpacecraftInImagePlane(double sampleDelta, double lineDelta)
    {
    	int nslices = image.getImageDepth();

        for (int slice = 0; slice < nslices; slice++)
        {
        	double[] sampleAxis = new double[] { 0.0, 0.0, 0.0 };
        	MathUtil.vsub(frustum1Adjusted[slice], frustum2Adjusted[slice], sampleAxis);
        	MathUtil.unorm(sampleAxis, sampleAxis);
        	double[] lineAxis = new double[] { 0.0, 0.0, 0.0 };
        	MathUtil.vsub(frustum1Adjusted[slice], frustum3Adjusted[slice], lineAxis);
        	MathUtil.unorm(lineAxis, lineAxis);
        	MathUtil.vscl(sampleDelta, sampleAxis, sampleAxis);
        	MathUtil.vadd(spacecraftPositionAdjusted[slice], sampleAxis, spacecraftPositionAdjusted[slice]);
        	MathUtil.vscl(lineDelta, lineAxis, lineAxis);
        	MathUtil.vadd(spacecraftPositionAdjusted[slice], lineAxis, spacecraftPositionAdjusted[slice]);
        }
    }

    void moveTargetPixelCoordinates(double[] pixelDelta)
    {
        double height = (double) image.getImageHeight();
        if (targetPixelCoordinates[0] == Double.MAX_VALUE || targetPixelCoordinates[1] == Double.MAX_VALUE)
        {
            targetPixelCoordinates = image.getPixelFromPoint(image.bodyOrigin);
            targetPixelCoordinates[0] = height - 1 - targetPixelCoordinates[0];
        }

        double line = this.targetPixelCoordinates[0] + pixelDelta[0];
        double sample = targetPixelCoordinates[1] + pixelDelta[1];
        double[] newFrustumCenterPixel = { line, sample };
        setTargetPixelCoordinates(newFrustumCenterPixel);
    }

    private void resetFrustaAndFootprint(int slice)
    {
    	image.getRendererHelper().resetFrustaAndFootprint(slice);
    }

    void movePitchAngleBy(double rotationDelta)
    {
    	setPitchOffset(pitchOffset[0] + rotationDelta);
    }

    void moveYawAngleBy(double rotationDelta)
    {
    	setYawOffset(yawOffset[0] + rotationDelta);
    }

    void moveLineOffsetBy(double offset)
    {
    	setLineOffset(lineOffset + offset);
    }

    void moveSampleOffsetBy(double offset)
    {
    	setSampleOffset(sampleOffset + offset);
    }

    /**
     * This adjusts the roll angle about the boresight direction
     * @param rotationDelta
     */
    void moveRotationAngleBy(double rotationDelta)
    {
        setRotationOffset(rotationOffset[0] + rotationDelta);
    }

    void moveZoomFactorBy(double zoomDelta)
    {
        setZoomFactor(zoomFactor[0] * zoomDelta);
    }

    // private void rotateBoresightDirectionTo(double[] newDirection)
    // {
    // Vector3D oldDirectionVector = new
    // Vector3D(boresightDirectionOriginal[currentSlice]);
    // Vector3D newDirectionVector = new Vector3D(newDirection);
    //
    // Rotation rotation = new Rotation(oldDirectionVector, newDirectionVector);
    //
    // int nslices = getNumberBands();
    // for (int i = 0; i<nslices; i++)
    // {
    // MathUtil.rotateVector(frustum1Adjusted[i], rotation, frustum1Adjusted[i]);
    // MathUtil.rotateVector(frustum2Adjusted[i], rotation, frustum2Adjusted[i]);
    // MathUtil.rotateVector(frustum3Adjusted[i], rotation, frustum3Adjusted[i]);
    // MathUtil.rotateVector(frustum4Adjusted[i], rotation, frustum4Adjusted[i]);
    // MathUtil.rotateVector(boresightDirectionAdjusted[i], rotation,
    // boresightDirectionAdjusted[i]);
    //
    // frusta[i] = null;
    // footprintGenerated[i] = false;
    // }
    //
    //// loadFootprint();
    //// calculateFrustum();
    // }

    void rotateTargetPixelDirectionToLocalOrigin(double[] direction)
    {
        Vector3D directionVector = new Vector3D(direction);
        Vector3D spacecraftPositionVector = new Vector3D(image.getSpacecraftPositionOriginal()[image.currentSlice]);
        Vector3D spacecraftToOriginVector = spacecraftPositionVector.scalarMultiply(-1.0);
        Vector3D originPointingVector = spacecraftToOriginVector.normalize();

        Rotation rotation = new Rotation(directionVector, originPointingVector);

        // int slice = getCurrentSlice();
        int nslices = image.getImageDepth();
        for (int slice = 0; slice < nslices; slice++)
        {
            MathUtil.rotateVector(frustum1Adjusted[slice], rotation, frustum1Adjusted[slice]);
            MathUtil.rotateVector(frustum2Adjusted[slice], rotation, frustum2Adjusted[slice]);
            MathUtil.rotateVector(frustum3Adjusted[slice], rotation, frustum3Adjusted[slice]);
            MathUtil.rotateVector(frustum4Adjusted[slice], rotation, frustum4Adjusted[slice]);
            MathUtil.rotateVector(boresightDirectionAdjusted[slice], rotation, boresightDirectionAdjusted[slice]);

            image.getRendererHelper().resetFrustaAndFootprint(slice);
        }
    }

    void resetInternalState()
    {
    	targetPixelCoordinates[0] = Double.MAX_VALUE;
        targetPixelCoordinates[1] = Double.MAX_VALUE;
        rotationOffset[0] = 0.0;
        zoomFactor[0] = 1.0;
        lineOffset = 0.0;
        sampleOffset = 0.0;
        pitchOffset[0] = 0.0;
        yawOffset[0] = 0.0;
        image.setUseDefaultFootprint(true);
    }
}