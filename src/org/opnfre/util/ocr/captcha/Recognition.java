package org.opnfre.util.ocr.captcha;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.imageio.ImageIO;

public class Recognition {
	private int absLevel = 3;

	private Float accuracy = new Float(0.9);

	private InputStream captchaIn;

	private int forePx = 0;

	private static Map<Integer, List<List<Integer>>> pxGridMem;
	private static Map<Integer, String> pxGridMemChar;
	private static Map<Integer, Integer> pxGridMemForePx;

	private static PreparedStatement pxGridSel;
	private static String pxGridSelSql = "SELECT * FROM pxgrid";

	private static PreparedStatement pxGridIns;
	private static String pxGridInsSql = "INSERT INTO `pxgrid` "
			+ "(`char`, `pxGrid`, `forePx`) VALUES(?, ?, ?)";

	static {
		try {
			Class.forName("org.sqlite.JDBC");
			String jdbcURL = "jdbc:sqlite:/home/liuchong/.data/"
					+ "ocr-captcha/ocr-captcha.sqlite";
			Connection dbConn = DriverManager.getConnection(jdbcURL);
			pxGridSel = dbConn.prepareStatement(pxGridSelSql);
			pxGridIns = dbConn.prepareStatement(pxGridInsSql);
			updatePxGridMem();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public Recognition(InputStream in) {
		captchaIn = in;
	}

	public Recognition(File file) throws FileNotFoundException {
		captchaIn = new FileInputStream(file);
	}

	private void addPxGrid(List<List<Integer>> onePxGrid, String ch)
			throws SQLException {
		int maxIndex = 0;
		for (Map.Entry<Integer, Integer> one : pxGridMemForePx.entrySet()) {
			int index = one.getKey();
			if (one.getKey() > maxIndex)
				maxIndex = index;
		}
		maxIndex++;
		pxGridMem.put(maxIndex, onePxGrid);
		pxGridMemChar.put(maxIndex, ch);
		pxGridMemForePx.put(maxIndex, forePx);
		pxGridIns.setString(1, ch);
		StringBuilder pxgrid = new StringBuilder();
		for (int y = 0; y < onePxGrid.size(); y++) {
			for (int x = 0; x < onePxGrid.get(y).size(); x++)
				pxgrid.append(onePxGrid.get(y).get(x).toString());
			pxgrid.append("\n");
		}
		pxGridIns.setString(2, pxgrid.toString());
		pxGridIns.setInt(3, forePx);
		pxGridIns.execute();
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
				if (forePx == gridPxPri[0][x]) {
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
				if (forePx == gridPxPri[lastYIndex][x]) {
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
				if (forePx == gridPxPri[y][0]) {
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
				if (forePx == gridPxPri[y][lastXIndex]) {
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

	private void display(List<List<Integer>> onePxGrid) {
		for (int y = 0; y < onePxGrid.size(); y++) {
			for (int x = 0; x < onePxGrid.get(y).size(); x++) {
				if (onePxGrid.get(y).get(x) == forePx)
					System.out.print(onePxGrid.get(y).get(x));
				else
					System.out.print(' ');
			}
			System.out.println();
		}
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
	private int[][] getPxGrid(InputStream in) throws IOException {
		BufferedImage image = ImageIO.read(in);
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
		in.close();
		return grid;
	}

	/**
	 * 提取指定范围的内的子像素网格
	 * 
	 * @param grid
	 *            - 父像素网格
	 * @param s
	 *            - 开始索引（横坐标）
	 * @param e
	 *            - 结束（横坐标）（不包含）
	 * @return 指定范围的内的 子 像素网格
	 */
	private List<List<Integer>> getSubPxGrid(int[][] grid, int s, int e) {
		List<List<Integer>> oneCharGrid = new ArrayList<List<Integer>>();
		for (int sy = 0; sy < grid.length; sy++) {
			ArrayList<Integer> oneLine = new ArrayList<Integer>(e - s);
			for (int sx = s; sx < e; sx++)
				oneLine.add(grid[sy][sx]);
			oneCharGrid.add(oneLine);
		}
		return oneCharGrid;
	}

	public String go() throws IOException, SQLException {
		int[][] grid = getPxGrid(captchaIn);
		int[][] gridPxAbs = absPxGrid(grid, absLevel);
		int[][] gridPxPri = cropPxGrid(gridPxAbs);
		List<List<List<Integer>>> gridPxSplit = splitPxGrid(gridPxPri);
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < gridPxSplit.size(); i++)
			result.append(matching(gridPxSplit.get(i)));
		return result.toString();
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
	 * 拆分裁切像素网格的匹配
	 * 
	 * @param grid
	 *            - 单个字符的像素网格
	 * @return 匹配到的字符
	 * @throws SQLException
	 *             - 如果存储目测结果时出错
	 */
	private String matching(List<List<Integer>> grid) throws SQLException {
		int pxCount = 0;
		for (int y = 0; y < grid.size(); y++)
			for (int x = 0; x < grid.get(0).size(); x++)
				if (grid.get(y).get(x) == forePx)
					pxCount++;
		Map<Float, String> result = new HashMap<Float, String>();
		for (Map.Entry<Integer, List<List<Integer>>> one : pxGridMem.entrySet()) {
			Integer oneId = one.getKey();
			Integer oneForePx = pxGridMemForePx.get(oneId);
			String oneChar = pxGridMemChar.get(oneId);
			List<List<Integer>> oneGrid = one.getValue();
			int onePxCount = 0;
			for (int y = 0; y < oneGrid.size(); y++)
				for (int x = 0; x < oneGrid.get(0).size(); x++)
					if (oneGrid.get(y).get(x).equals(oneForePx))
						onePxCount++;
			int same = 0;
			for (int y = 0; y < oneGrid.size() && y < grid.size(); y++) {
				for (int x = 0; x < oneGrid.get(y).size(); x++) {
					if (x >= grid.get(y).size())
						break;
					if (grid.get(y).get(x) == forePx)
						if (oneGrid.get(y).get(x).equals(oneForePx))
							same++;
				}
			}
			Float oneResult = new Float((same * 1.0)
					/ ((pxCount + onePxCount) / 2.0));
			result.put(oneResult, oneChar);
		}
		Map.Entry<Float, String> best = null;
		for (Map.Entry<Float, String> one : result.entrySet()) {
			if (best == null) {
				best = one;
				continue;
			}
			if (best.getKey() < one.getKey())
				best = one;
		}
		display(grid);
		System.out.println("最匹配的结果：" + best);
		String ch = "";
		if (best == null || best.getKey().compareTo(accuracy) < 1) {
			System.out.println("没有匹配项目，请输入目测结果：");
			Scanner cin = new Scanner(System.in);
			ch = cin.nextLine();
			addPxGrid(grid, ch);
		} else {
			ch = best.getValue();
		}
		return ch;
	}

	/**
	 * 拆分像素网格
	 * 
	 * @param grid
	 *            - 像素网格（最好是先抽象后裁切过的）
	 * @return 拆分后的像素网格
	 */
	private List<List<List<Integer>>> splitPxGrid(int[][] grid) {
		List<List<List<Integer>>> gridPxSplit = new ArrayList<List<List<Integer>>>();
		// 纵向扫描分割行
		int prevSplitPosX = 0;
		for (int x = 0; x < grid[0].length; x++) {
			boolean bgLine = true;
			for (int y = 0; y < grid.length; y++) {
				if (forePx == grid[y][x]) {
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
					if (forePx == grid[y][x]) {
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

	private static void updatePxGridMem() throws SQLException {
		ResultSet rs = pxGridSel.executeQuery();
		Map<Integer, List<List<Integer>>> newPxGridMem;
		Map<Integer, String> newPxGridMemChar;
		Map<Integer, Integer> newPxGridMemForePx;
		newPxGridMem = new HashMap<Integer, List<List<Integer>>>();
		newPxGridMemChar = new HashMap<Integer, String>();
		newPxGridMemForePx = new HashMap<Integer, Integer>();
		while (rs.next()) {
			int id = rs.getInt("id");
			String ch = rs.getString("char");
			String[] pxGrid = rs.getString("pxgrid").split("\n");
			int oneForePx = rs.getInt("forePx");
			List<List<Integer>> onePxGrid = new ArrayList<List<Integer>>();
			for (int i = 0; i < pxGrid.length; i++) {
				List<Integer> line = new ArrayList<Integer>();
				char[] pxLine = pxGrid[i].toCharArray();
				for (int j = 0; j < pxLine.length; j++) {
					Integer onePx = new Integer(pxLine[j] + "");
					line.add(onePx);
				}
				onePxGrid.add(line);
			}
			newPxGridMem.put(id, onePxGrid);
			newPxGridMemChar.put(id, ch);
			newPxGridMemForePx.put(id, oneForePx);
		}
		rs.close();
		pxGridMem = newPxGridMem;
		pxGridMemChar = newPxGridMemChar;
		pxGridMemForePx = newPxGridMemForePx;
	}

	public static void main(String args[]) throws IOException, SQLException {
		ArrayList<String> captchas = new ArrayList<String>();
		captchas.add("/home/liuchong/Pictures/Web/captcha/v6ta");

	}
}