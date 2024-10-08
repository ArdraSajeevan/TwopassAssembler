import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

public class AssemblerGUI extends JFrame {
  
    private JTextArea sourceProgramArea, optabArea, symbolTableArea, intermediateFileArea, objectCodeArea, objectCodePerLineArea;

    private HashMap<String, Integer> symbolTable = new HashMap<>();
    private HashMap<String, String> optab = new HashMap<>();

    private String programName = "";
    private String startingAddress = "";
    private int programLength = 0;

    public AssemblerGUI() {
        
        setTitle("Two-Pass Assembler");
        setSize(1000, 700); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null); 

        sourceProgramArea = new JTextArea();
        JScrollPane sourceScrollPane = new JScrollPane(sourceProgramArea);
        sourceScrollPane.setBounds(50, 50, 400, 150); 
        add(sourceScrollPane);

        JLabel sourceLabel = new JLabel("Source Program");
        sourceLabel.setBounds(50, 20, 200, 20);
        add(sourceLabel);
   
        optabArea = new JTextArea();
        JScrollPane optabScrollPane = new JScrollPane(optabArea);
        optabScrollPane.setBounds(500, 50, 400, 150); 
        add(optabScrollPane);

        JLabel optabLabel = new JLabel("Operation Table (OPTAB)");
        optabLabel.setBounds(500, 20, 200, 20);
        add(optabLabel);
  
        symbolTableArea = new JTextArea();
        symbolTableArea.setEditable(false);
        JScrollPane symbolScrollPane = new JScrollPane(symbolTableArea);
        symbolScrollPane.setBounds(50, 250, 400, 150); 
        add(symbolScrollPane);

        JLabel symbolLabel = new JLabel("Symbol Table");
        symbolLabel.setBounds(50, 220, 200, 20);
        add(symbolLabel);

        intermediateFileArea = new JTextArea();
        intermediateFileArea.setEditable(false);
        JScrollPane intermediateScrollPane = new JScrollPane(intermediateFileArea);
        intermediateScrollPane.setBounds(500, 250, 400, 150); 
        add(intermediateScrollPane);

        JLabel intermediateLabel = new JLabel("Intermediate File");
        intermediateLabel.setBounds(500, 220, 200, 20);
        add(intermediateLabel);
  
        objectCodeArea = new JTextArea();
        objectCodeArea.setEditable(false);
        JScrollPane objectScrollPane = new JScrollPane(objectCodeArea);
        objectScrollPane.setBounds(50, 450, 400, 150); 
        add(objectScrollPane);

        JLabel objectLabel = new JLabel("Object Code");
        objectLabel.setBounds(50, 420, 200, 20);
        add(objectLabel);

        objectCodePerLineArea = new JTextArea();
        objectCodePerLineArea.setEditable(false);
        JScrollPane objectPerLineScrollPane = new JScrollPane(objectCodePerLineArea);
        objectPerLineScrollPane.setBounds(500, 450, 400, 150); 
        add(objectPerLineScrollPane);

        JLabel objectPerLineLabel = new JLabel("Object Code for Each Line");
        objectPerLineLabel.setBounds(500, 420, 200, 20);
        add(objectPerLineLabel);

        JButton runPass1Button = new JButton("Run Pass 1");
        runPass1Button.setBounds(300, 620, 150, 30); 
        add(runPass1Button);
        runPass1Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runPass1();
            }
        });

        JButton runPass2Button = new JButton("Run Pass 2");
        runPass2Button.setBounds(500, 620, 150, 30); 
        add(runPass2Button);
        runPass2Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runPass2();
            }
        });

        setVisible(true);
    }


    private void runPass1() {
        try {
            
            symbolTableArea.setText("");
            intermediateFileArea.setText("");

            String[] sourceProgramLines = sourceProgramArea.getText().split("\n");

            String[] optabLines = optabArea.getText().split("\n");
            optab.clear();
            for (String line : optabLines) {
                String[] parts = line.split("\\s+");
                optab.put(parts[0], parts[1]);
            }

            int locctr = 0;
            boolean startSeen = false;
            int startingAddressInt = 0;
            int lastInstructionAddress = 0;

            StringBuilder intermediateFile = new StringBuilder();
            StringBuilder symtabOutput = new StringBuilder();

            for (String line : sourceProgramLines) {
                String[] parts = line.trim().split("\\s+");
                String label = "", opcode = "", operand = "";

                if (parts.length == 3) {
                    label = parts[0];
                    opcode = parts[1];
                    operand = parts[2];
                } else if (parts.length == 2) {
                    opcode = parts[0];
                    operand = parts[1];
                } else if (parts.length == 1) {
                    opcode = parts[0];
                }

                
                if (opcode.equals("START")) {
                    programName = label;
                    locctr = Integer.parseInt(operand, 16);
                    startingAddress = operand;
                    startingAddressInt = locctr;
                    startSeen = true;
                    intermediateFile.append(String.format("%s %s\n", locctr == 0 ? "" : String.format("%04X", locctr), line));
                    continue;
                }

                if (!label.isEmpty()) {
                    symbolTable.put(label, locctr);
                    symtabOutput.append(String.format("%s %04X\n", label, locctr));
                }

                intermediateFile.append(String.format("%04X %s\n", locctr, line));

                if (optab.containsKey(opcode)) {
                    lastInstructionAddress = locctr;  
                    locctr += 3;  
                } else if (opcode.equals("WORD")) {
                    locctr += 3;
                } else if (opcode.equals("RESW")) {
                    locctr += 3 * Integer.parseInt(operand);
                } else if (opcode.equals("RESB")) {
                    locctr += Integer.parseInt(operand);
                } else if (opcode.equals("BYTE")) {
                    if (operand.startsWith("C'") && operand.endsWith("'")) {
                        locctr += operand.length() - 3;  
                    } else if (operand.startsWith("X'") && operand.endsWith("'")) {
                        locctr += (operand.length() - 3) / 2; 
                    }
                } else if (opcode.equals("END")) {
                    break;
                }
            }

            programLength = lastInstructionAddress - startingAddressInt;

            symbolTableArea.setText(symtabOutput.toString());
            intermediateFileArea.setText(intermediateFile.toString());

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error during Pass 1: " + e.getMessage());
        }
    }

    private void runPass2() {
        try {
            objectCodeArea.setText("");
            objectCodePerLineArea.setText("");

            String[] symtabLines = symbolTableArea.getText().split("\n");
            symbolTable.clear();
            for (String line : symtabLines) {
                String[] parts = line.split("\\s+");
                symbolTable.put(parts[0], Integer.parseInt(parts[1], 16));
            }

            String[] intermediateFileLines = intermediateFileArea.getText().split("\n");

            StringBuilder objCodeOutput = new StringBuilder();
            StringBuilder objCodePerLineOutput = new StringBuilder();
            StringBuilder textRecord = new StringBuilder();

            objCodeOutput.append(String.format("H^%-6s^%06X^%06X\n", programName, Integer.parseInt(startingAddress, 16), programLength));

            String currentTextRecord = "";
            int currentTextStartAddress = -1;
            int currentTextLength = 0;

            for (String line : intermediateFileLines) {
                String[] parts = line.trim().split("\\s+");
                String address = parts[0];
                String label = "", opcode = "", operand = "";

                if (parts.length == 4) {
                    label = parts[1];
                    opcode = parts[2];
                    operand = parts[3];
                } else if (parts.length == 3) {
                    opcode = parts[1];
                    operand = parts[2];
                } else if (parts.length == 2) {
                    opcode = parts[1];
                }

                String objCode = "";  

                if (optab.containsKey(opcode)) {
                    objCode = optab.get(opcode);
                    String operandAddress = "0000";

                    if (!operand.isEmpty() && symbolTable.containsKey(operand)) {
                        operandAddress = String.format("%04X", symbolTable.get(operand));
                    }

                    objCode += operandAddress;
                } else if (opcode.equals("WORD")) {
                    objCode = String.format("%06X", Integer.parseInt(operand));
                } else if (opcode.equals("BYTE")) {
                    if (operand.startsWith("C'") && operand.endsWith("'")) {
                        String constant = operand.substring(2, operand.length() - 1);
                        for (int i = 0; i < constant.length(); i++) {
                            objCode += String.format("%02X", (int) constant.charAt(i));
                        }
                    } else if (operand.startsWith("X'") && operand.endsWith("'")) {
                        objCode = operand.substring(2, operand.length() - 1);
                    }
                }

                if (!objCode.isEmpty()) {
                    if (currentTextStartAddress == -1) {
                        currentTextStartAddress = Integer.parseInt(address, 16);
                    }

                    if (currentTextLength + objCode.length() / 2 > 30) {
                        objCodeOutput.append(String.format("T^%06X^%02X%s\n", currentTextStartAddress, currentTextLength, currentTextRecord));
                        currentTextRecord = "";
                        currentTextStartAddress = Integer.parseInt(address, 16);
                        currentTextLength = 0;
                    }

                    currentTextRecord += "^" + objCode;
                    currentTextLength += objCode.length() / 2;

                    objCodePerLineOutput.append(String.format("%04X %s %s\n", Integer.parseInt(address, 16), line, objCode));
                } else if (opcode.equals("END")) {
                    if (!currentTextRecord.isEmpty()) {
                        objCodeOutput.append(String.format("T^%06X^%02X%s\n", currentTextStartAddress, currentTextLength, currentTextRecord));
                    }
                    objCodeOutput.append(String.format("E^%06X\n", Integer.parseInt(startingAddress, 16)));
                    break;
                }
            }

            objectCodeArea.setText(objCodeOutput.toString());
            objectCodePerLineArea.setText(objCodePerLineOutput.toString());

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error during Pass 2: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new AssemblerGUI();
            }
        });
    }
} 