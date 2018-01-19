package services.moleculer.repl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TextTable {

	// --- VARIABLES ---

	private int padding = 2;
	private String newLine = System.getProperty("line.separator", "\r\n");

	private char jointSymbol = '+';
	private char vSplitSymbol = '|';
	private char hSplitSymbol = '-';

	// --- CONSTRUCTOR ---

	private final boolean drawGridAndHeader;

	protected final List<String> headersList;

	public TextTable(String... headers) {
		this(true, headers);
	}

	public TextTable(boolean drawGridAndHeader, String... headers) {
		this.drawGridAndHeader = drawGridAndHeader;
		this.headersList = Arrays.asList(headers);
	}

	// --- ROWS AND CELLS ---

	private List<List<String>> rowsList = new LinkedList<>();

	public void addRow(String... cells) {
		addRow(false, Arrays.asList(cells));
	}

	public void addRow(boolean checkLength,String... cells) {
		addRow(checkLength, Arrays.asList(cells));
	}

	public void addRow(List<String> cells) {
		addRow(false, cells);
	}
	
	public void addRow(boolean checkLength, List<String> cells) {
		String last = cells.get(cells.size() - 1);
		if (!checkLength || last.length() <= 40) {
			rowsList.add(cells);
		} else {
			boolean first = true;
			while (!last.isEmpty()) {
				int max = Math.min(last.length(), 40);
				String part = last.substring(0, max);
				last = last.substring(max);
				if (first) {
					first = false;
					cells.set(cells.size() - 1, part);
					rowsList.add(cells);
				} else {
					LinkedList<String> list = new LinkedList<>();
					for (int i = 0; i < cells.size() - 1; i++) {
						list.add("");
					}
					list.add(part);
					rowsList.add(list);
				}
			}
		}
	}

	// --- GENERATE TABLE ---

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		Map<Integer, Integer> columnMaxWidthMapping = getMaximumWidhtofTable(headersList, rowsList);
		if (drawGridAndHeader) {
			createRowLine(stringBuilder, headersList.size(), columnMaxWidthMapping);
			stringBuilder.append(newLine);
			for (int headerIndex = 0; headerIndex < headersList.size(); headerIndex++) {
				fillCell(stringBuilder, headersList.get(headerIndex), headerIndex, columnMaxWidthMapping, true);
			}
			stringBuilder.append(newLine);
			createRowLine(stringBuilder, headersList.size(), columnMaxWidthMapping);
			stringBuilder.append(newLine);
		}
		for (List<String> row : rowsList) {
			for (int cellIndex = 0; cellIndex < row.size(); cellIndex++) {
				fillCell(stringBuilder, row.get(cellIndex), cellIndex, columnMaxWidthMapping, false);
			}
			stringBuilder.append(newLine);
		}
		if (drawGridAndHeader) {
			createRowLine(stringBuilder, headersList.size(), columnMaxWidthMapping);
		}
		return stringBuilder.toString();
	}

	protected void fillSpace(StringBuilder stringBuilder, int length) {
		for (int i = 0; i < length; i++) {
			stringBuilder.append(" ");
		}
	}

	protected void createRowLine(StringBuilder stringBuilder, int headersListSize,
			Map<Integer, Integer> columnMaxWidthMapping) {
		for (int i = 0; i < headersListSize; i++) {
			if (i == 0) {
				stringBuilder.append(jointSymbol);
			}
			for (int j = 0; j < columnMaxWidthMapping.get(i) + padding * 2; j++) {
				stringBuilder.append(hSplitSymbol);
			}
			stringBuilder.append(jointSymbol);
		}
	}

	protected Map<Integer, Integer> getMaximumWidhtofTable(List<String> headersList, List<List<String>> rowsList) {
		Map<Integer, Integer> columnMaxWidthMapping = new HashMap<>();
		for (int columnIndex = 0; columnIndex < headersList.size(); columnIndex++) {
			columnMaxWidthMapping.put(columnIndex, 0);
		}
		for (int columnIndex = 0; columnIndex < headersList.size(); columnIndex++) {
			if (headersList.get(columnIndex).length() > columnMaxWidthMapping.get(columnIndex)) {
				columnMaxWidthMapping.put(columnIndex, headersList.get(columnIndex).length());
			}
		}
		for (List<String> row : rowsList) {
			for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
				if (row.get(columnIndex).length() > columnMaxWidthMapping.get(columnIndex)) {
					columnMaxWidthMapping.put(columnIndex, row.get(columnIndex).length());
				}
			}
		}
		for (int columnIndex = 0; columnIndex < headersList.size(); columnIndex++) {
			if (columnMaxWidthMapping.get(columnIndex) % 2 != 0) {
				columnMaxWidthMapping.put(columnIndex, columnMaxWidthMapping.get(columnIndex) + 1);
			}
		}
		return columnMaxWidthMapping;
	}

	protected int getOptimumCellPadding(int cellIndex, int datalength, Map<Integer, Integer> columnMaxWidthMapping,
			int cellPaddingSize) {
		if (datalength % 2 != 0) {
			datalength++;
		}
		if (datalength < columnMaxWidthMapping.get(cellIndex)) {
			cellPaddingSize = cellPaddingSize + (columnMaxWidthMapping.get(cellIndex) - datalength) / 2;
		}
		return cellPaddingSize;
	}

	protected void fillCell(StringBuilder stringBuilder, String cell, int cellIndex,
			Map<Integer, Integer> columnMaxWidthMapping, boolean centered) {
		int cellPaddingSize = getOptimumCellPadding(cellIndex, cell.length(), columnMaxWidthMapping, padding);
		if (cellIndex == 0 && drawGridAndHeader) {
			stringBuilder.append(vSplitSymbol);
		}
		if (centered) {
			fillSpace(stringBuilder, cellPaddingSize);
			stringBuilder.append(cell);
			if (cell.length() % 2 != 0) {
				stringBuilder.append(' ');
			}
			fillSpace(stringBuilder, cellPaddingSize);
		} else {
			fillSpace(stringBuilder, padding);
			stringBuilder.append(cell);
			if (cell.length() % 2 != 0) {
				stringBuilder.append(' ');
			}
			fillSpace(stringBuilder, (2 * cellPaddingSize) - padding);
		}
		if (drawGridAndHeader) {
			stringBuilder.append(vSplitSymbol);
		}
	}

	// --- SETTERS ---

	public void setPadding(int padding) {
		this.padding = padding;
	}

	public void setNewLine(String newLine) {
		this.newLine = newLine;
	}

	public void setJointSymbol(char jointSymbol) {
		this.jointSymbol = jointSymbol;
	}

	public void setvSplitSymbol(char vSplitSymbol) {
		this.vSplitSymbol = vSplitSymbol;
	}

	public void sethSplitSymbol(char hSplitSymbol) {
		this.hSplitSymbol = hSplitSymbol;
	}

}