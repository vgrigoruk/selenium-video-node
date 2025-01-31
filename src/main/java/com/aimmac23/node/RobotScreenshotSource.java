package com.aimmac23.node;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import com.aimmac23.node.jna.EncoderInterface;
import com.aimmac23.node.jna.JnaLibraryLoader;
import com.sun.jna.Pointer;

public class RobotScreenshotSource implements ScreenshotSource {

	private Robot robot;

	public RobotScreenshotSource() throws Exception {
		robot = new Robot();
	}
	
	@Override
	public int applyScreenshot(Pointer encoderContext) {
		BufferedImage image = robot.createScreenCapture(getScreenSize());

		int[] screenshotData = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
		
		EncoderInterface encoder = JnaLibraryLoader.getEncoder();

		return encoder.convert_frame(encoderContext, screenshotData);
	}

	@Override
	public int getWidth() {
		return getScreenSize().width;
	}

	@Override
	public int getHeight() {
		return getScreenSize().height;
	}

	protected Rectangle getScreenSize() {
		//XXX: This probably won't work with multiple monitors
		return GraphicsEnvironment.getLocalGraphicsEnvironment().
				getDefaultScreenDevice().getDefaultConfiguration().getBounds();
		
	}

	@Override
	public void doStartupSanityChecks() {
		// nothing to do - creating the Robot should have done this
	}
}
