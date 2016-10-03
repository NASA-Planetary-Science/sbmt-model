package edu.jhuapl.sbmt.model.europa.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class BlockFile {
	public RandomAccessFile fileDataInput = null;
	public String fullPath = null;
	public int blockSize = 4096;
	public byte block[] = null;
	protected int blockOffset = 0;
	protected int fileSize = 0;
	protected int fileDataSize = 0;
	protected int labelOffset = 0;
	
	public BlockFile()
	{
	}
	public BlockFile(String filename, int offset) throws IOException
	{
		labelOffset = offset;
		openFile(filename);
	}
	public BlockFile(String filename, int blkSize, int offset) throws IOException
	{
		labelOffset = offset;
		blockSize = blkSize;
		openFile(filename);
	}
	public void openFile(String filename) throws IOException
	{
		if (fileDataInput == null)
		{
			File f = new File(filename);
			if (f.exists())
			{
				fileDataInput = new RandomAccessFile(filename, "r");
				if (fileDataInput != null)
				{
					fileSize = (int)fileDataInput.length();
					fileDataSize = fileSize - labelOffset;
					if (fileDataSize < blockSize)
						blockSize = fileDataSize;
					fullPath = filename;
				}
			}
		}
	}
	public void setBlockData(byte data[])
	{
		//
		//	no checking for size or consistency here
		//
		blockOffset = 0;
		blockSize = data.length;
		block = data;
		fileSize = fileDataSize = data.length;
		labelOffset = 0;
	}
	public byte[] getBlockData()
	{
		return block;
	}
	public void setBlock(int offset)
	{
		
	}
	public boolean isInBlock(int offset)
	{
		boolean result = false;
		
		return result;
	}
	protected void readBlock(int offset, int dataLength) throws IOException
	{	
		int off = 0;
		if ((offset > (blockOffset + blockSize)) || (offset < blockOffset) || ((offset+dataLength-1) > (blockOffset+blockSize)))
		{
			if ((offset + blockSize) > fileDataSize)
			{
				off = fileDataSize - blockSize;
			}
			else
			{
				off = offset - blockSize/2;
			}
		}
		
		if (off < 0) off = 0;
		
		if (block == null)
			block = new byte[blockSize];
		
		blockOffset = off;
		
		fileDataInput.seek(blockOffset + labelOffset);
		fileDataInput.read(block, 0, blockSize);
	}
	public void readEntireFile() throws IOException
	{
		if ((fileDataSize > 0) && (fileDataInput != null))
		{
			blockSize = fileDataSize;
			block = new byte[blockSize];
			
			blockOffset = 0;
			fileDataInput.seek(labelOffset);
			fileDataInput.read(block, 0, blockSize);
			fileDataInput.close();
			fileDataInput = null;
		}
	}
	public boolean haveEntireFile()
	{
		return (fileDataSize == blockSize);
	}
	public int getFileSize(){ return fileDataSize;}
	public void openFile() throws IOException
	{
		openFile(fullPath);
	}
	public void closeFile() throws IOException
	{
		if (isOpen())
		{
			fileDataInput.close();
			fileDataInput = null;
		}
		block = null;
		fileSize = fileDataSize = blockSize = 0;
	}
	public String getFullPath(){ return fullPath;}
	public boolean isOpen()
	{
		return (fileDataInput != null);
	}
	public boolean readBytes(int offset, byte data[]) throws IOException
	{
		boolean result = false;
		
		if ((block ==null) || ((offset+data.length-1) > (blockOffset+blockSize)) || (offset > (blockOffset+blockSize)) || (offset < blockOffset))
		{
			readBlock(offset, data.length);
		}
		
		if ((offset + data.length - 1) < (blockOffset + block.length - 1))
		{
			for (int i = 0; i < data.length; i++)
				data[i] = block[offset-blockOffset+i];
			
			result = true;
		}
		
		return result;
	}
	public byte readByte(int offset) throws IOException
	{
		byte result = 0;
		
		if ((block ==null) || (offset > (blockOffset+blockSize)) || (offset < blockOffset))
		{
			readBlock(offset, 1);
		}
		result = block[offset - blockOffset];
		
		return result;
	}
	public short readShort(int offset) throws IOException
	{
		short result = 0;
		
		if ((block == null) || ((offset+1) > (blockOffset+blockSize)) || (offset < blockOffset))
		{
			readBlock(offset, 2);
		}
		byte b1 = block[offset - blockOffset];
		byte b2 = block[offset - blockOffset + 1];
		
		//result |= block[offset - blockOffset] << 8;
		//result |= block[offset - blockOffset + 1];
		result = (short)(((b1&0xff)<<8) | (b2&0xff));
		
		return result;
	}
	public float readFloat(int offset) throws IOException
	{
		float result = 0;
		
		if ((block ==null) || ((offset+3) > (blockOffset+blockSize)) || (offset < blockOffset))
		{
			readBlock(offset, 4);
		}
		
		result = Float.intBitsToFloat((block[offset - blockOffset + 3]&0xff) |
				((block[offset - blockOffset + 2]&0xff) << 8) |
				((block[offset - blockOffset + 1]&0xff) << 16) |
				((block[offset - blockOffset]&0xff) << 24));
		
		return result;
	}
	public float readPCFloat(int offset) throws IOException
	{
		float result = 0;
		
		if ((block ==null) || ((offset+3) > (blockOffset+blockSize)) || (offset < blockOffset))
		{
			readBlock(offset, 4);
		}
		
		result = Float.intBitsToFloat((block[offset - blockOffset]&0xff) |
				((block[offset - blockOffset + 1]&0xff) << 8) |
				((block[offset - blockOffset + 2]&0xff) << 16) |
				((block[offset - blockOffset + 3]&0xff) << 24));
		
		return result;
	}
	public int readInt(int offset) throws IOException
	{
		int result = 0;
		
		if ((block ==null) || ((offset+3) > (blockOffset+blockSize)) || (offset < blockOffset))
		{
			readBlock(offset, 4);
		}
		
		int byte1 = block[offset - blockOffset];
		int byte2 = block[offset - blockOffset + 1];
		int byte3 = block[offset - blockOffset + 2];
		int byte4 = block[offset - blockOffset + 3];
		
		result = (byte4&0xff) | ((byte3&0xff) << 8) | ((byte2&0xff) << 16) | ((byte1&0xff) << 24);
		
		return result;
	}
	public double readDouble(int offset) throws IOException
	{
		double result = 0;
		
		if ((block ==null) || ((offset+7) > (blockOffset+blockSize)) || (offset < blockOffset))
		{
			readBlock(offset, 8);
		}
		
		long byte1 = block[offset - blockOffset];
		long byte2 = block[offset - blockOffset + 1];
		long byte3 = block[offset - blockOffset + 2];
		long byte4 = block[offset - blockOffset + 3];
		long byte5 = block[offset - blockOffset + 4];
		long byte6 = block[offset - blockOffset + 5];
		long byte7 = block[offset - blockOffset + 6];
		long byte8 = block[offset - blockOffset + 7];
		
		long bits = (byte8&0xff) | ((byte7&0xff) << 8) | ((byte6&0xff) << 16) | ((byte5&0xff) << 24) |
			((byte4&0xff) << 32) | ((byte3&0xff) << 40) | ((byte2&0xff) << 48) | ((byte1&0xff) << 56);
		
		result = Double.longBitsToDouble(bits);
		
		return result;
	}
}
