package pencarian_rute;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.Stack;
import javax.swing.*;
import static javax.swing.JFrame.EXIT_ON_CLOSE;
import javax.swing.event.*;

public class Pencarian_Rute {
    public static JFrame mazeFrame;  // The main form of the program
    
    public Pencarian_Rute() {
    }
    public static void main(String[] args) {
        int width  = 800;
        int height = 470;
        mazeFrame = new JFrame("PENCARIAN RUTE PENGANTARAN MAKANAN CEPAT SAJI TERDEKAT");
        mazeFrame.setContentPane(new MazePanel(width,height));
        mazeFrame.pack();
        mazeFrame.setResizable(false);
        
        ImageIcon ico = new ImageIcon("src/pencarian_rute/logoP.png");
        mazeFrame.setIconImage(ico.getImage());

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        double screenWidth = screenSize.getWidth();
        double ScreenHeight = screenSize.getHeight();
        int x = ((int)screenWidth-width)/2;
        int y = ((int)ScreenHeight-height)/2;
        
        mazeFrame.setLocation(x,y);
        mazeFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mazeFrame.setVisible(true);
    } // end main()

    public static class MazePanel extends JPanel {
        private class Cell {
            int row;   
            int col;  
            int g;     
            int h;     
            int f;   
            int dist;
            Cell prev;
            public Cell(int row, int col){
               this.row = row;
               this.col = col;
            }
        } // end nested class Cell

        private class CellComparatorByF implements Comparator<Cell>{
            @Override
            public int compare(Cell cell1, Cell cell2){
                return cell1.f-cell2.f;
            }
        } // end nested class CellComparatorByF

        private class CellComparatorByDist implements Comparator<Cell>{
            @Override
            public int compare(Cell cell1, Cell cell2){
                return cell1.dist-cell2.dist;
            }
        } // end nested class CellComparatorByDist
 
        private class MouseHandler implements MouseListener, MouseMotionListener {
            private int cur_row, cur_col, cur_val;
            @Override
            public void mousePressed(MouseEvent evt) {
                int row = (evt.getY() - 10) / squareSize;
                int col = (evt.getX() - 10) / squareSize;
                if (row >= 0 && row < rows && col >= 0 && col < columns) {
                    if (realTime ? true : !found && !searching){

                        if (realTime) {
                            searching = true;
                            fillGrid();
                            map();
                        }
                        cur_row = row;
                        cur_col = col;
                        cur_val = grid[row][col];
                    }
                }
                if (realTime) {
                    timer.setDelay(0);
                    timer.start();
                    checkTermination();
                } else {
                    repaint();
                }
            }

            @Override
            public void mouseDragged(MouseEvent evt) {
                int row = (evt.getY() - 10) / squareSize;
                int col = (evt.getX() - 10) / squareSize;
                if (row >= 0 && row < rows && col >= 0 && col < columns){
                    if (realTime ? true : !found && !searching){
                        if (realTime) {
                            searching = true;
                            fillGrid();
                        }
                        if ((row*columns+col != cur_row*columns+cur_col) && (cur_val == ROBOT || cur_val == TARGET)){
                            int new_val = grid[row][col];
                            if (new_val == EMPTY){
                                grid[row][col] = cur_val;
                                if (cur_val == TARGET) {
                                    targetPos.row = row;
                                    targetPos.col = col;
                                }
                                grid[cur_row][cur_col] = new_val;
                                cur_row = row;
                                cur_col = col;
                               if (cur_val == TARGET)  {
                                    targetPos.row = cur_row;
                                    targetPos.col = cur_col;
                                }
                                cur_val = grid[row][col];
                            }
                        } 
                    }
                }
                if (realTime) {
                    timer.setDelay(0);
                    timer.start();
                    checkTermination();
                } else {
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent evt) { }
            @Override
            public void mouseEntered(MouseEvent evt) { }
            @Override
            public void mouseExited(MouseEvent evt) { }
            @Override
            public void mouseMoved(MouseEvent evt) { }
            @Override
            public void mouseClicked(MouseEvent evt) { }
            
        } // end nested class MouseHandler

        private class ActionHandler implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent evt) {
                String cmd = evt.getActionCommand();
                if (cmd.equals("RESTART")) {
                    fillGrid();
                    map();
                    realTime = false;
                    realTimeButton.setEnabled(true);
                    realTimeButton.setForeground(Color.black);
                    stepButton.setEnabled(true);
                    animationButton.setEnabled(true);
                    slider.setEnabled(true);
                    dfs.setEnabled(true);
                    bfs.setEnabled(true);
                    aStar.setEnabled(true);
                    drawArrows.setEnabled(true);
                } else if (cmd.equals("X")) {
                    int selectedOption = JOptionPane.showConfirmDialog(null,
                        "Apakah anda akan menutup aplikasi?", "Tutup Aplikasi", JOptionPane.YES_NO_OPTION);
                        if (selectedOption == JOptionPane.YES_OPTION) {
                            System.exit(0);
                        }
                } else if (cmd.equals("Step-by-Step") && !found && !endOfSearch) {
                } else if (cmd.equals("START") && !endOfSearch) {
                    realTime = false;
                    searching = true;
                    realTimeButton.setEnabled(false);
                    dfs.setEnabled(false);
                    bfs.setEnabled(false);
                    aStar.setEnabled(false);
                    drawArrows.setEnabled(false);
                    timer.setDelay(delay);
                    timer.start();
                } 
            }
        } // end nested class ActionHandler
   

        private class RepaintAction implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent evt) {
                checkTermination();
                if (found) {
                    timer.stop();
                }
                if (!realTime) {
                    repaint();
                }
            }
        } // end nested class RepaintAction
      
        public void checkTermination() {
                expandNode();
                if (found) {
                    endOfSearch = true;
                    plotRoute();
                    stepButton.setEnabled(false);
                    animationButton.setEnabled(false);
                    slider.setEnabled(false);
                    repaint();
                }
    
        }

        private class MyMaze {
            private int dimensionX, dimensionY;
            private int gridDimensionX, gridDimensionY; 
            private char[][] mazeGrid; 
            private Cell[][] cells; 
            private Random random = new Random(); 

            public MyMaze(int aDimension) {
                this(aDimension, aDimension);
            }
            // constructor
            public MyMaze(int xDimension, int yDimension) {
                dimensionX = xDimension;
                dimensionY = yDimension;
                gridDimensionX = xDimension * 2 + 1;
                gridDimensionY = yDimension * 2 + 1;
                mazeGrid = new char[gridDimensionX][gridDimensionY];
                init();
                generateMaze();
            }

            private void init() {
                cells = new Cell[dimensionX][dimensionY];
                for (int x = 0; x < dimensionX; x++) {
                    for (int y = 0; y < dimensionY; y++) {
                        cells[x][y] = new Cell(x, y, false); // create cell (see Cell constructor)
                    }
                }
            }

            private class Cell {
                int x, y;
                ArrayList<Cell> neighbors = new ArrayList<>();
                boolean wall = true;
                boolean open = true;
                Cell(int x, int y) {
                    this(x, y, true);
                }
                Cell(int x, int y, boolean isWall) {
                    this.x = x;
                    this.y = y;
                    this.wall = isWall;
                }

                void addNeighbor(Cell other) {
                    if (!this.neighbors.contains(other)) { 
                        this.neighbors.add(other);
                    }
                    if (!other.neighbors.contains(this)) { 
                        other.neighbors.add(this);
                    }
                }

                boolean isCellBelowNeighbor() {
                    return this.neighbors.contains(new Cell(this.x, this.y + 1));
                }

                boolean isCellRightNeighbor() {
                    return this.neighbors.contains(new Cell(this.x + 1, this.y));
                }

                @Override
                public boolean equals(Object other) {
                    if (!(other instanceof Cell)) return false;
                    Cell otherCell = (Cell) other;
                    return (this.x == otherCell.x && this.y == otherCell.y);
                }

                @Override
                public int hashCode() {
                    return this.x + this.y * 256;
                }
            }

            private void generateMaze() {
                generateMaze(0, 0);
            }

            private void generateMaze(int x, int y) {
                generateMaze(getCell(x, y)); 
            }
            private void generateMaze(Cell startAt) {
                if (startAt == null) return;
                startAt.open = false; 
                ArrayList<Cell> cellsList = new ArrayList<>();
                cellsList.add(startAt);
            }
            // used to get a Cell at x, y; returns null out of bounds
            public Cell getCell(int x, int y) {
                try {
                    return cells[x][y];
                } catch (ArrayIndexOutOfBoundsException e) { 
                    return null;
                }
            }
        } // end nested class MyMaze

        private final static int
            INFINITY = Integer.MAX_VALUE, 
            EMPTY    = 0,  
            OBST     = 1, 
            ROBOT    = 2,  
            TARGET   = 3,  
            FRONTIER = 4,  
            CLOSED   = 5,  
            ROUTE    = 6,  
            TOKO     = 7;
        
        // Messages to the user
        private final static String
            pesan =
                "Drag posisi Tujuan Anda : Warna ",
            warnaT =
                "Orange";
        
        JSpinner rowsSpinner, columnsSpinner; 
        
        int rows    = 11,            //Lebar Map
            columns = 15,           //Panjang Map
            squareSize = 428/rows;  //Dimensi Grid
        

        int arrowSize = squareSize/2;
        ArrayList<Cell> openSet   = new ArrayList();
        ArrayList<Cell> closedSet = new ArrayList();
        ArrayList<Cell> graph     = new ArrayList();
         
        Cell robotStart; // the initial position of the robot
        Cell targetPos;  // the position of the target
      
        JLabel message,message1;  // message to the user
        
        // basic buttons
        JButton resetButton, mazeButton, clearButton, realTimeButton, stepButton, animationButton,closeButton;
        
        // buttons for selecting the algorithm   
        JCheckBox bfs,dfs,aStar;
        
        // the slider for adjusting the speed of the animation
        JSlider slider;

        // Draw arrows to predecessors
        JCheckBox drawArrows;

        int[][] grid;     
        boolean realTime;    
        boolean found;      
        boolean searching;  
        boolean endOfSearch; 
        int delay;           
        int expanded;  
        private Image gambarIcon,gambarB,gambarP,gambarBP,gambarTaman,gambarBangunan1,gambarBangunan2,
                gambarBangunan3,gambarPohon,gambarJ,gambarPanel,gambarShop,gambarToko1,gambarToko2,gambarJalanV,gambarJalanH,gambarTujuan,
                gambarGridHT,gambarBangunan4,gambarBangunan5,gambarBangunan6,gambarBangunan7,gambarBangunan8,gambarBangunan9,
                gambarBangunan10,gambarBangunan11,gambarBangunan12,gambarToko;
        
        // the object that controls the animation
        RepaintAction action = new RepaintAction();
        
        // the Timer which governs the execution speed of the animation
        Timer timer;
        
        public MazePanel(int width, int height) {
            setLayout(null);
            
            MouseHandler listener = new MouseHandler();
            addMouseListener(listener);
            addMouseMotionListener(listener);

//            setBorder(BorderFactory.createMatteBorder(2,2,2,2,Color.DARK_GRAY));

            setPreferredSize( new Dimension(width,height) );

            grid = new int[rows][columns];

            // We create the contents of the panel
            mazeFrame.setUndecorated(true);
            message = new JLabel(pesan, JLabel.LEFT);
            message.setForeground(Color.BLACK);
            message.setFont(new Font("Helvetica",Font.PLAIN,16));
            
            message1 = new JLabel(warnaT, JLabel.LEFT);
            message1.setForeground(Color.ORANGE);
            message1.setFont(new Font("Helvetica",Font.PLAIN,16));
            
            gambarIcon = new ImageIcon(getClass().getResource("LogoK.png")).getImage();
            gambarBP = new ImageIcon(getClass().getResource("PanelBP.png")).getImage();
            gambarB = new ImageIcon(getClass().getResource("PanelBB.png")).getImage();
            gambarP = new ImageIcon(getClass().getResource("PanelP.png")).getImage();
            gambarJ = new ImageIcon(getClass().getResource("PanelJ.png")).getImage();
            gambarTaman = new ImageIcon(getClass().getResource("taman.png")).getImage();
            gambarBangunan1 = new ImageIcon(getClass().getResource("bangunan1.png")).getImage();
            gambarBangunan2 = new ImageIcon(getClass().getResource("bangunan2.png")).getImage();
            gambarBangunan3 = new ImageIcon(getClass().getResource("bangunan3.png")).getImage();
            gambarPohon = new ImageIcon(getClass().getResource("pohon4.png")).getImage();
            gambarPanel = new ImageIcon(getClass().getResource("PanelPP.png")).getImage();
            gambarShop= new ImageIcon(getClass().getResource("shop3.png")).getImage();
            gambarJalanV= new ImageIcon(getClass().getResource("jalanV.png")).getImage();
            gambarJalanH= new ImageIcon(getClass().getResource("jalanH.png")).getImage();
            gambarGridHT= new ImageIcon(getClass().getResource("poin3.png")).getImage();
            gambarTujuan= new ImageIcon(getClass().getResource("tujuan.png")).getImage();
            gambarBangunan4= new ImageIcon(getClass().getResource("bangunan4.jpg")).getImage();
            gambarBangunan5= new ImageIcon(getClass().getResource("bangunan5.jpg")).getImage();
            gambarBangunan6= new ImageIcon(getClass().getResource("bangunan6.png")).getImage();
            gambarBangunan8=new ImageIcon(getClass().getResource("bangunan8.png")).getImage();
            gambarBangunan9=new ImageIcon(getClass().getResource("bangunan9.png")).getImage();
            gambarBangunan10=new ImageIcon(getClass().getResource("bangunan10.png")).getImage();
            gambarBangunan11=new ImageIcon(getClass().getResource("bangunan11.png")).getImage();
            gambarBangunan12=new ImageIcon(getClass().getResource("bangunan12.png")).getImage();
            gambarToko=new ImageIcon(getClass().getResource("Toko.png")).getImage();

            resetButton = new JButton("New grid");
            resetButton.addActionListener(new ActionHandler());
            resetButton.setBackground(Color.lightGray);
            resetButton.setToolTipText
                    ("Clears and redraws the grid according to the given rows and columns");
            resetButton.addActionListener(this::resetButtonActionPerformed);

//            mazeButton = new JButton("Maze");
            closeButton = new JButton("X");
            closeButton.addActionListener(new ActionHandler());
            closeButton.setBackground(Color.lightGray);
            closeButton.setFont(new Font("Perfect Dark (BRK)",Font.PLAIN,12));
            closeButton.setToolTipText
                    ("Keluar Dari Aplikasi");
            
            clearButton = new JButton("RESTART");
            clearButton.addActionListener(new ActionHandler());
            clearButton.setBackground(Color.lightGray);
            clearButton.setFont(new Font("Perfect Dark (BRK)",Font.PLAIN,12));
            clearButton.setToolTipText
                    ("Ulang Pencarian Rute");

            realTimeButton = new JButton("Real-Time");
            realTimeButton.addActionListener(new ActionHandler());
            realTimeButton.setBackground(Color.lightGray);
            realTimeButton.setToolTipText
                    ("Position of obstacles, robot and target can be changed when search is underway");

            stepButton = new JButton("Step-by-Step");
            stepButton.addActionListener(new ActionHandler());
            stepButton.setBackground(Color.lightGray);
            stepButton.setToolTipText
                    ("The search is performed step-by-step for every click");

            animationButton = new JButton("START");
            animationButton.addActionListener(new ActionHandler());
            animationButton.setBackground(Color.lightGray);
            animationButton.setFont(new Font("Perfect Dark (BRK)",Font.PLAIN,12));
            animationButton.setToolTipText
                    ("Memulai Pencarian Rute");

            JLabel velocity = new JLabel("ALGORITMA", JLabel.CENTER);
            velocity.setFont(new Font("Imprint MT Shadow",Font.PLAIN,14));
            
            JLabel judul1 = new JLabel("PENCARIAN RUTE", JLabel.CENTER);
            judul1.setFont(new Font("Perfect Dark (BRK)",Font.PLAIN,12));
            JLabel judul2 = new JLabel("PENGANTARAN MAKANAN", JLabel.CENTER);
            judul2.setFont(new Font("Perfect Dark (BRK)",Font.PLAIN,12));
            JLabel judul3 = new JLabel("CEPAT SAJI", JLabel.CENTER);
            judul3.setFont(new Font("Perfect Dark (BRK)",Font.PLAIN,12));
            
            JLabel judul4 = new JLabel("Status : ", JLabel.LEFT);
            judul4.setFont(new Font("Helvetica",Font.BOLD,16));
            
            slider = new JSlider(0,1000,500); // initial value of delay 500 msec
            slider.setBackground(new Color(89,126,172));
            slider.setToolTipText
                    ("Mempercepat Pencarian");
            
            delay = 1000-slider.getValue();
            slider.addChangeListener((ChangeEvent evt) -> {
                JSlider source = (JSlider)evt.getSource();
                if (!source.getValueIsAdjusting()) {
                    delay = 1000-source.getValue();
                }
            });

            ButtonGroup algoGroup = new ButtonGroup();
            
            bfs = new JCheckBox("BFS");
            bfs.setToolTipText("Algoritma Breadth First Search");
            algoGroup.add(bfs);
            bfs.addActionListener(new ActionHandler());
            bfs.setBackground(new Color(89,126,172));
            
            dfs = new JCheckBox("DFS");
            dfs.setToolTipText("Algoritma Depth First Search");
            algoGroup.add(dfs);
            dfs.addActionListener(new ActionHandler());
            dfs.setBackground(new Color(89,126,172));

            aStar = new JCheckBox("A*");
            aStar.setToolTipText("Algoritma A*");
            algoGroup.add(aStar);
            aStar.addActionListener(new ActionHandler());
            aStar.setBackground(new Color(89,126,172));
            
            bfs.setSelected(true);  // DFS is initially selected 

            drawArrows = new
                    JCheckBox("Arrows to predecessors");
            drawArrows.setToolTipText("Draw arrows to predecessors");

            // we add the contents of the panel
            add(message);
            add(message1);
            add(closeButton);
            add(resetButton);
            add(clearButton);
            add(realTimeButton);
            add(stepButton);
            add(animationButton);
            add(judul1);
            add(judul2);
            add(judul3);
            add(velocity);
            add(judul4);
            add(slider);
            add(dfs);
            add(bfs);
            add(aStar);
            add(drawArrows);

            // we regulate the sizes and positions
            message.setBounds(70, 440, 500, 23);
            message1.setBounds(305, 440, 500, 23);
            closeButton.setBounds(745,8,45,30);
//            judul1.setBounds(600, 15, 200, 14);
//            judul2.setBounds(600, 35, 200, 14);
//            judul3.setBounds(600, 55, 200, 14);
            
            judul4.setBounds(10, 440, 500, 23);
            
            velocity.setBounds(600, 140, 100, 14);
            bfs.setBounds(600, 170, 70, 25);
            dfs.setBounds(600, 195, 70, 25);
            aStar.setBounds(600, 220, 70, 25);
            slider.setBounds(600, 260, 190, 25);
            
            animationButton.setBounds(602, 345, 88, 25);
            clearButton.setBounds(692, 345, 95, 25);
            
            timer = new Timer(delay, action);

            fillGrid();
            map();

        } // end constructor

    static protected JSpinner addLabeledSpinner(Container c,
                                                String label,
                                                SpinnerModel model) {
        JLabel l = new JLabel(label);
        c.add(l);
 
        JSpinner spinner = new JSpinner(model);
        l.setLabelFor(spinner);
        c.add(spinner);
 
        return spinner;
    }

        private void resetButtonActionPerformed(java.awt.event.ActionEvent evt) {                                           
            realTime = false;
            realTimeButton.setEnabled(true);
            realTimeButton.setForeground(Color.black);
            stepButton.setEnabled(true);
            animationButton.setEnabled(true);
            slider.setEnabled(true);
        } // end resetButtonActionPerformed()
    
        private void mazeButtonActionPerformed(java.awt.event.ActionEvent evt) {
            realTime = false;
            realTimeButton.setEnabled(true);
            realTimeButton.setForeground(Color.black);
            stepButton.setEnabled(true);
            animationButton.setEnabled(true);
            slider.setEnabled(true);
        } // end mazeButtonActionPerformed()


        private void expandNode(){
            Cell current;
            if (dfs.isSelected() || bfs.isSelected()) {
                current = openSet.remove(0);
            } else {
                Collections.sort(openSet, new CellComparatorByF());
                current = openSet.remove(0);
            }

            closedSet.add(0,current);
            grid[current.row][current.col] = CLOSED;

            if (current.row == targetPos.row && current.col == targetPos.col) {
                Cell last = targetPos;
                last.prev = current.prev;
                closedSet.add(last);
                found = true;
                return;
            }
            expanded++;

            ArrayList<Cell> succesors;
            succesors = createSuccesors(current, false);
            succesors.stream().forEach((cell) -> {
                if (dfs.isSelected()) {
                    openSet.add(0, cell);
                    grid[cell.row][cell.col] = FRONTIER;
                } else if (bfs.isSelected()){
                    openSet.add(cell);
                    grid[cell.row][cell.col] = FRONTIER;
                } else if (aStar.isSelected()){
                    int dxg = current.col-cell.col;
                    int dyg = current.row-cell.row;
                    int dxh = targetPos.col-cell.col;
                    int dyh = targetPos.row-cell.row;
                    cell.g = current.g+Math.abs(dxg)+Math.abs(dyg);
                    cell.h = Math.abs(dxh)+Math.abs(dyh);
                    cell.f = cell.g+cell.h;
                    int openIndex   = isInList(openSet,cell);
                    int closedIndex = isInList(closedSet,cell);
                    if (openIndex == -1 && closedIndex == -1) {
                        openSet.add(cell);
                        grid[cell.row][cell.col] = FRONTIER;
                    } else {
                        if (openIndex > -1){
                            if (openSet.get(openIndex).f <= cell.f) {
                            } else {
                                openSet.remove(openIndex);
                                openSet.add(cell);
                                grid[cell.row][cell.col] = FRONTIER;
                            }
                        } else {
                            if (closedSet.get(closedIndex).f <= cell.f) {
                            } else {
                                closedSet.remove(closedIndex);
                                openSet.add(cell);
                                grid[cell.row][cell.col] = FRONTIER;
                            }
                        }
                    }
                }
            });
        } //end expandNode()

        private ArrayList<Cell> createSuccesors(Cell current, boolean makeConnected){
            int r = current.row;
            int c = current.col;
            ArrayList<Cell> temp = new ArrayList<>();

            if (r > 0 && grid[r-1][c] != OBST &&
                    ((aStar.isSelected()) ? true :
                          isInList(openSet,new Cell(r-1,c)) == -1 &&
                          isInList(closedSet,new Cell(r-1,c)) == -1)) {
                Cell cell = new Cell(r-1,c);
                cell.prev = current;
                temp.add(cell);
            }
            if (c < columns-1 && grid[r][c+1] != OBST &&
                    ((aStar.isSelected())? true :
                          isInList(openSet,new Cell(r,c+1)) == -1 &&
                          isInList(closedSet,new Cell(r,c+1)) == -1)) {
                Cell cell = new Cell(r,c+1);
                cell.prev = current;
                temp.add(cell);
            }

            if (r < rows-1 && grid[r+1][c] != OBST &&
                    ((aStar.isSelected()) ? true :
                          isInList(openSet,new Cell(r+1,c)) == -1 &&
                          isInList(closedSet,new Cell(r+1,c)) == -1)) {
                Cell cell = new Cell(r+1,c);
                cell.prev = current;
                temp.add(cell);
            }

            if (c > 0 && grid[r][c-1] != OBST && 
                    ((aStar.isSelected()) ? true :
                          isInList(openSet,new Cell(r,c-1)) == -1 &&
                          isInList(closedSet,new Cell(r,c-1)) == -1)) {
                Cell cell = new Cell(r,c-1);
                cell.prev = current;
                temp.add(cell);
            }

            if (dfs.isSelected()){
                Collections.reverse(temp);
            }
            return temp;
        } // end createSuccesors()

        private int isInList(ArrayList<Cell> list, Cell current){
            int index = -1;
            for (int i = 0 ; i < list.size(); i++) {
                if (current.row == list.get(i).row && current.col == list.get(i).col) {
                    index = i;
                    break;
                }
            }
            return index;
        } // end isInList()

        private Cell findPrev(ArrayList<Cell> list, Cell current){
            int index = isInList(list, current);
            return list.get(index).prev;
        } // end findPrev()

        private void plotRoute(){
            searching = false;
            endOfSearch = true;
            int steps = 0;
            double distance = 0;
            int index = isInList(closedSet,targetPos);
            Cell cur = closedSet.get(index);
            grid[cur.row][cur.col]= TARGET;
            do {
                steps++; 
                distance++;
                cur = cur.prev;
                grid[cur.row][cur.col] = ROUTE;
            } while (!(cur.row == robotStart.row && cur.col == robotStart.col));
            grid[robotStart.row][robotStart.col]=ROBOT;
//            String msg;
            message.setText("Pencarian Suskes !!!");
            message1.setText(" ");
//            JOptionPane.showMessageDialog(null, message);//pop up
          
        } // end plotRoute()
        
        private void fillGrid() {
            if (searching || endOfSearch){ 
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < columns; c++) {
                        if (grid[r][c] == FRONTIER || grid[r][c] == CLOSED || grid[r][c] == ROUTE) {
                            grid[r][c] = EMPTY;
                        }
                        if (grid[r][c] == ROBOT){
                            robotStart = new Cell(r,c);
                        }
                        if (grid[r][c] == TARGET){
                            targetPos = new Cell(r,c);
                        }
                    }
                }
                searching = false;
            } else {
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < columns; c++) {
                        grid[r][c] = EMPTY;
                    }
                }
                robotStart = new Cell(rows-1,6);
                targetPos = new Cell(2,columns-15);
            }
            if (aStar.isSelected()){
                robotStart.g = 0;
                robotStart.h = 0;
                robotStart.f = 0;
            }
            expanded = 0;
            found = false;
            searching = false;
            endOfSearch = false;

            openSet.removeAll(openSet);
            openSet.add(robotStart);
            closedSet.removeAll(closedSet);
         
            grid[targetPos.row][targetPos.col] = TARGET; 
            grid[robotStart.row][robotStart.col] = ROBOT;
            message.setText(pesan);
            message1.setText(warnaT);
            timer.stop();
            repaint();
            
        } // end fillGrid()
        
        public void map(){
            //baris 1
            for(int i=0;i<columns;i++){
                if(i==3||i==7||i==11){
                    continue;
                }
                grid[0][i]=OBST;
            }
            //baris 2
            for(int i=0;i<columns;i++){
                if(i==3||i==7||i==11){
                    continue;
                }
                grid[1][i]=OBST;
            }
            //baris 3
//            for(int i=0;i<columns;i++){
//                if(i==2||i==5||i==8){
//                    continue;
//                }
//                grid[2][i]=OBST;
//            }
            //baris 4
            for(int i=0;i<columns;i++){
                if(i==3||i==11){
                    continue;
                }
                grid[3][i]=OBST;
            }
            //baris 5
            for(int i=0;i<columns;i++){
                if(i==3||i==11){
                    continue;
                }
                grid[4][i]=OBST;
            }
            //baris 6
            for(int i=0;i<columns;i++){
                if(i==3||i==11){
                    continue;
                }
                grid[5][i]=OBST;
            }
            //baris 7
            for(int i=0;i<columns;i++){
                if(i==3||i==11){
                    continue;
                }
                grid[7][i]=OBST;
            }
            //baris 10
            for(int i=0;i<columns;i++){
                if(i==2||i==5||i==11){
                    continue;
                }
                grid[9][i]=OBST;
            }
            //baris 11
            for(int i=0;i<columns;i++){
                if(i==2||i==5||i==11){
                    continue;
                }
                if(i==6){
                    grid[10][i]=TOKO;
                    continue;
                }
                grid[10][i]=OBST;
            }
        }

        private void findConnectedComponent(Cell v){
            Stack<Cell> stack;
            stack = new Stack();
            ArrayList<Cell> succesors;
            stack.push(v);
            graph.add(v);
            while(!stack.isEmpty()){
                v = stack.pop();
                succesors = createSuccesors(v, true);
                for (Cell c: succesors) {
                    if (isInList(graph, c) == -1){
                        stack.push(c);
                        graph.add(c);
                    }
                }
            }
        } // end findConnectedComponent()

        @Override
        public void paintComponent(Graphics g) {

            super.paintComponent(g);  // Fills the background color.
            g.drawImage(gambarPanel, 0, 0,830,500, null);//pabel belakang
            g.drawImage(gambarJ, 5, 5,582,431,null);//panel jalan
            
            g.drawImage(gambarJalanV, 5, 97,100,20,null);//panel jalanK
            g.drawImage(gambarJalanV, 107, 97,100,20,null);//panel jalanK
            g.drawImage(gambarJalanV, 209, 97,100,20,null);//panel jalanK
            g.drawImage(gambarJalanV, 310, 97,100,20,null);//panel jalanK
            g.drawImage(gambarJalanV, 411, 97,100,20,null);//panel jalanK
            g.drawImage(gambarJalanV, 485, 97,100,20,null);//panel jalanK
            g.drawImage(gambarJalanV, 5, 250,100,20,null);//panel jalanK
            g.drawImage(gambarJalanV, 107, 250,100,20,null);//panel jalanK
            g.drawImage(gambarJalanV, 209, 250,100,20,null);//panel jalanK
            g.drawImage(gambarJalanV, 310, 250,100,20,null);//panel jalanK
            g.drawImage(gambarJalanV, 411, 250,100,20,null);//panel jalanK
            g.drawImage(gambarJalanV, 485, 250,100,20,null);//panel jalanK
            g.drawImage(gambarJalanV, 5, 325,100,20,null);//panel jalanK
            g.drawImage(gambarJalanV, 107, 325,100,20,null);//panel jalanK
            g.drawImage(gambarJalanV, 209, 325,100,20,null);//panel jalanK
            g.drawImage(gambarJalanV, 310, 325,100,20,null);//panel jalanK
            g.drawImage(gambarJalanV, 411, 325,100,20,null);//panel jalanK
            g.drawImage(gambarJalanV, 485, 325,100,20,null);//panel jalanK
            
            g.drawImage(gambarJalanH, 135, 5,20,100,null);//panel jalanK
            g.drawImage(gambarJalanH, 135, 104,20,100,null);//panel jalanK
            g.drawImage(gambarJalanH, 135, 206,20,100,null);//panel jalanK
            g.drawImage(gambarJalanH, 135, 233,20,100,null);//panel jalanK
            g.drawImage(gambarJalanH, 440, 5,20,100,null);//panel jalanK
            g.drawImage(gambarJalanH, 440, 104,20,100,null);//panel jalanK
            g.drawImage(gambarJalanH, 440, 206,20,100,null);//panel jalanK
            g.drawImage(gambarJalanH, 440, 308,20,100,null);//panel jalanK
            g.drawImage(gambarJalanH, 440, 335,20,100,null);//panel jalanK
            
            g.drawImage(gambarJalanH, 285, 5,20,100,null);//panel jalanK
            g.drawImage(gambarJalanH, 95, 335,20,100,null);//panel jalanK
            g.drawImage(gambarJalanH, 210, 335,20,100,null);//panel jalanK
            
            g.drawImage(gambarBP, 595, 5,200,35,null);//panel judul
            g.drawImage(gambarBP, 595, 45,200,70,null);//panel judul
            g.drawImage(gambarBP, 595, 120,200,190,null);//panel algoritma
            g.drawImage(gambarBP, 595, 315,200,85,null);//panel tombol
            g.drawImage(gambarBP, 5, 440,790,25,null);//panel status
            
            
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < columns; c++) {
                    if (grid[r][c] == EMPTY) {
                        g.setColor(new Color(0,0,0,0));
                    }else if (grid[r][c] == ROBOT) {
                        g.setColor(new Color(142,180,227));  
                    }else if (grid[r][c] == TARGET) {
                        g.setColor(new Color(255,192,0));
                    } else if (grid[r][c] == OBST) {
                        g.setColor(new Color(0,0,0,0));
//                        g.setColor(new Color(198,217,241));
                    } else if (grid[r][c] == FRONTIER) {
                        g.setColor(new Color(79,98,40,200));
                    } else if (grid[r][c] == CLOSED) {
                        if(r==10 && c==6){
                            g.setColor(new Color(142,180,227));
                        }
                        g.setColor(new Color(155,187,89,100));
                    } else if (grid[r][c] == ROUTE) {
                        if(r==10 && c==6){
                            g.setColor(new Color(142,180,227));
                        }
                        g.setColor(new Color(240,255,0,200));
                    }else if (grid[r][c] == TOKO) {
                        g.setColor(new Color(142,180,227));
                    }
                    g.fillRect(11 + c*squareSize, 11 + r*squareSize, squareSize, squareSize);
                    
                }
            }
            g.drawImage(gambarIcon, 630, 50,150,65,null);
            g.drawImage(gambarTaman, 163, 114,266,125,null);
            g.drawImage(gambarBangunan1, 11, 125,115,115,null);
            g.drawImage(gambarBangunan2, 467, 125,115,115,null);
            g.drawImage(gambarPohon, 467, 3,120,85,null);
            g.drawImage(gambarBangunan3, 237, 335,197,94,null);
            g.drawImage(gambarBangunan5, 10, 277,115,38,null);
            g.drawImage(gambarPohon, 11, 345,80,84,null);
            g.drawImage(gambarBangunan2, 125, 353,80,76,null);
            g.drawImage(gambarBangunan9, 467, 353,114,76,null);
            g.drawImage(gambarBangunan8, 162, 277,267,38,null);
            g.drawImage(gambarPohon, 467, 272,117,44,null);
            g.drawImage(gambarBangunan4, 10, 12,115,75,null);
            g.drawImage(gambarBangunan10, 163, 12,114,75,null);
            g.drawImage(gambarBangunan11, 315, 12,115,75,null);
            g.drawImage(gambarToko, 243, 370,65,50,null);
//            g.drawImage(gifCircle, 243, 370,100,100,null);
//            
        } // end paintComponent()      
    } // end nested classs MazePanel
} // end class Maze
    
