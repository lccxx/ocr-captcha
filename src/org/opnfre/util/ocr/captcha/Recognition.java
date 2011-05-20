package org.opnfre.util.ocr.captcha;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

public class Recognition {
	private int bgPx = 1;

	private String filename;

	public Recognition(String filename) {
		this.filename = filename;
	}

	public String go() throws IOException {
		String result = "";
		int[][] grid = getPxGrid(filename);
		int[][] gridPxAbs = absPxGrid(grid, 2);
		int[][] gridPxPri = cropPxGrid(gridPxAbs);
		ArrayList<ArrayList<ArrayList<Integer>>> gridPxSplit = splitPxGrid(gridPxPri);

		System.out.println("图片被抽象后（抽象像素网格）：");
		for (int y = 0; y < gridPxAbs.length; y++) {
			for (int x = 0; x < gridPxAbs[0].length; x++)
				System.out.print(gridPxAbs[y][x]);
			System.out.println();
		}

		System.out.println("抽象像素网格被裁切后：");
		for (int y = 0; y < gridPxPri.length; y++) {
			for (int x = 0; x < gridPxPri[0].length; x++)
				System.out.print(gridPxPri[y][x]);
			System.out.println();
		}

		System.out.println("拆分裁切后的抽象像素网格：");
		for (int i = 0; i < gridPxSplit.size(); i++) {
			for (int y = 0; y < gridPxSplit.get(i).size(); y++) {
				for (int x = 0; x < gridPxSplit.get(i).get(y).size(); x++) {
					if (gridPxSplit.get(i).get(y).get(x)==0) 
						System.out.print(gridPxSplit.get(i).get(y).get(x));
					else
						System.out.print(' ');
				}
				System.out.println();
			}
			System.out.println();
		}
		return result;
	}

	/**
	 * 对像素进行抽象网格进行抽象（见 hashPixel）
	 * 
	 * @param grid
	 *            - 待抽象的像素网格
	 * @param level
	 *            - 抽象等级，值越小抽象层次越高，最小 2
	 * @return 抽象后的像素网格
	 */
	private int[][] absPxGrid(int[][] grid, int level) {
		int[][] gridPxAbs = new int[grid.length][grid[0].length];
		for (int y = 0; y < grid.length; y++)
			for (int x = 0; x < grid[0].length; x++)
				gridPxAbs[y][x] = hashPixel(grid[y][x], level);
		return gridPxAbs;
	}

	/**
	 * 对像素网格进行裁切
	 * 
	 * @param grid
	 *            - 待裁切的像素网格
	 * @return 裁切后的像素网格
	 */
	private int[][] cropPxGrid(int[][] grid) {
		int[][] gridPxPri = grid;
		// 从上面裁切
		while (true) {
			boolean bgLine = true;
			for (int x = 0; x < gridPxPri[0].length; x++) {
				if (bgPx != gridPxPri[0][x]) {
					bgLine = false;
					break;
				}
			}
			if (!bgLine)
				break;
			int[][] newGrid = new int[gridPxPri.length - 1][gridPxPri[0].length];
			for (int y = 1; y < gridPxPri.length; y++)
				for (int x = 0; x < gridPxPri[0].length; x++)
					newGrid[y - 1][x] = gridPxPri[y][x];
			gridPxPri = newGrid;
		}
		// 从下面裁切
		while (true) {
			int lastYIndex = gridPxPri.length - 1;
			boolean bgLine = true;
			for (int x = gridPxPri[lastYIndex].length - 1; x >= 0; x--) {
				if (bgPx != gridPxPri[lastYIndex][x]) {
					bgLine = false;
					break;
				}
			}
			if (!bgLine)
				break;
			int[][] newGrid = new int[gridPxPri.length - 1][gridPxPri[0].length];
			for (int y = 0; y < gridPxPri.length - 1; y++)
				for (int x = 0; x < gridPxPri[0].length; x++)
					newGrid[y][x] = gridPxPri[y][x];
			gridPxPri = newGrid;
		}
		// 从左边裁切
		while (true) {
			boolean bgLine = true;
			for (int y = 0; y < gridPxPri.length; y++) {
				if (bgPx != gridPxPri[y][0]) {
					bgLine = false;
					break;
				}
			}
			if (!bgLine)
				break;
			int[][] newGrid = new int[gridPxPri.length][gridPxPri[0].length - 1];
			for (int y = 0; y < gridPxPri.length; y++)
				for (int x = 1; x < gridPxPri[0].length; x++)
					newGrid[y][x - 1] = gridPxPri[y][x];
			gridPxPri = newGrid;
		}
		// 从右边裁切
		while (true) {
			int lastYIndex = gridPxPri.length - 1;
			int lastXIndex = gridPxPri[0].length - 1;
			boolean bgLine = true;
			for (int y = lastYIndex; y >= 0; y--) {
				if (bgPx != gridPxPri[y][lastXIndex]) {
					bgLine = false;
					break;
				}
			}
			if (!bgLine)
				break;
			int[][] newGrid = new int[gridPxPri.length][lastXIndex];
			for (int y = 0; y < gridPxPri.length; y++)
				for (int x = 0; x < gridPxPri[0].length - 1; x++)
					newGrid[y][x] = gridPxPri[y][x];
			gridPxPri = newGrid;
		}
		return gridPxPri;
	}

	/**
	 * 获取图片的像素网格（二维数组）
	 * 
	 * @param filename
	 *            - 图片的文件名
	 * @return 像素网格（二维数组）
	 * @throws IOException
	 *             - 如果图片文件读取错误
	 */
	private int[][] getPxGrid(String filename) throws IOException {
		BufferedImage image = ImageIO.read(new File(filename));
		int iw = image.getWidth();
		int ih = image.getHeight();
		int[][] grid = new int[ih][iw];
		int[] pixels = new int[iw * ih];
		PixelGrabber pg = new PixelGrabber(image.getSource(), 0, 0, iw, ih,
				pixels, 0, iw);
		try {
			pg.grabPixels();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		int x = 0, y = 0;
		for (int i = 0; i < pixels.length; i++) {
			grid[y][x++] = pixels[i];
			if ((i + 1) % iw == 0) {
				y++;
				x = 0;
			}
		}
		return grid;
	}

	/**
	 * 提取指定范围的内的子像素网格
	 * @param grid - 父像素网格
	 * @param s - 开始索引（横坐标）
	 * @param e - 结束（横坐标）（不包含）
	 * @return 指定范围的内的 子 像素网格
	 */
	private ArrayList<ArrayList<Integer>> getSubPxGrid(int[][] grid, int s,
			int e) {
		ArrayList<ArrayList<Integer>> oneCharGrid = new ArrayList<ArrayList<Integer>>();
		for (int sy = 0; sy < grid.length; sy++) {
			ArrayList<Integer> oneLine = new ArrayList<Integer>(e - s);
			for (int sx = s; sx < e; sx++)
				oneLine.add(grid[sy][sx]);
			oneCharGrid.add(oneLine);
		}
		return oneCharGrid;
	}

	/**
	 * 对像素进行抽象
	 * 
	 * @param pixel
	 *            - 待抽象的像素
	 * @param level
	 *            - 抽象等级，值越小抽象层次越高，最小 2
	 * @return 抽象后的像素值
	 */
	private int hashPixel(int pixel, int level) {
		ColorModel cm = ColorModel.getRGBdefault();
		int red = cm.getRed(pixel);
		int green = cm.getGreen(pixel);
		int blue = cm.getBlue(pixel);
		int hash = (red + green + blue) / 3;
		int factor = 255 / level;
		for (int i = 0; i < level; i++) {
			if (i * factor <= hash && hash <= (i + 1) * factor)
				return i;
		}
		return level - 1;
	}

	/**
	 * 拆分像素网格
	 * 
	 * @param grid
	 *            - 像素网格（最好是先抽象后裁切过的）
	 * @return 拆分后的像素网格
	 */
	private ArrayList<ArrayList<ArrayList<Integer>>> splitPxGrid(int[][] grid) {
		ArrayList<ArrayList<ArrayList<Integer>>> gridPxSplit = new ArrayList<ArrayList<ArrayList<Integer>>>();
		// 纵向扫描分割行
		int prevSplitPosX = 0;
		for (int x = 0; x < grid[0].length; x++) {
			boolean bgLine = true;
			for (int y = 0; y < grid.length; y++) {
				if (bgPx != grid[y][x]) {
					bgLine = false;
					break;
				}
			}
			if (!bgLine)
				continue;
			int splitX = x;
			// 继续扫描分割行，直到找到一个不是分割行的行
			for (; x < grid[0].length; x++) {
				for (int y = 0; y < grid.length; y++) {
					if (bgPx != grid[y][x]) {
						bgLine = false;
						break;
					}
				}
				if (!bgLine)
					break;
			}
			gridPxSplit.add(getSubPxGrid(grid, prevSplitPosX, splitX));
			prevSplitPosX = x;
		}
		gridPxSplit.add(getSubPxGrid(grid, prevSplitPosX, grid[0].length));
		return gridPxSplit;
	}
	
	public static void main(String args[]) throws IOException {
		String filename = "/home/liuchong/Pictures/Web/captcha.png";
		Recognition r = new Recognition(filename);
		r.go();
	}
}
