/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sqlbird;
import java.sql.*;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class WorkBench extends javax.swing.JFrame {
    /*Atributos*/
    Connection con;                                                             //Conexão BD
    String url, usuario, senha;                                                 //Strings
    DefaultListModel modelLista1, modelLista2, modelLista3, modelLista4;        //Referente as listas
    DefaultTableModel modelTabela1;
    Statement stmt;
    Statement stmtAux;
    ResultSet rs;
    ResultSetMetaData rsmd;
    boolean limite;
    
    int tipoBD;// 0 nenhum tipo de banco
               // 1 é mysql
               // 2 é postgre
    int indiceLista1Atual;
    int indiceLista2Atual;
    
    
    public WorkBench(){
        modelLista1 = new DefaultListModel();
        modelLista2 = new DefaultListModel();
        modelLista3 = new DefaultListModel();
        modelLista4 = new DefaultListModel();
        modelTabela1 = new DefaultTableModel();
        
        initComponents();
        
        limparLista(jList1, modelLista1);
        limparLista(jList2, modelLista2); 
        limparLista(jList3, modelLista3);
        limparLista(jList4, modelLista4);
        limparTabela(jTable2, modelTabela1);
        indiceLista1Atual = -1;
        indiceLista2Atual = -1;
        limite = true;
    }
    
    
    /*Função que identifica o tipo do banco(MySQL ou Postgre)*/
    public int TipoBD(String url){
        int tipoBDaux, tipoBD;
        boolean mysql = false;
        
        tipoBDaux = url.indexOf("mysql");
        if(tipoBDaux > 0){
            tipoBD = 1; //é mysql
            mysql = true;
        }
        else{
            tipoBD = 0; //é nenhum
        }
        
        tipoBDaux = url.indexOf("postgre");
        if(tipoBDaux > 0 && mysql == false){
            tipoBD = 2; //é postgre
        }
        else{
            if(mysql == false){
                tipoBD = 0; //é nenhum
            }
        }
        return tipoBD;
    }
    
    /*Seleciona o banco inicial que o WookBench trabalha.*/
    public void setConexao(String urlP, String usuarioP, String senhaP) throws SQLException{
        url = urlP;
        usuario = usuarioP;
        senha = senhaP;
        con = DriverManager.getConnection(url, usuario, senha);
        stmt = con.createStatement();
        stmtAux = con.createStatement();
        
        tipoBD = TipoBD(url);
        atualizaLista1();
    } 
    
    /*Funções de manipulação das tabelas*/
    public void limparTabela(JTable tableP, DefaultTableModel modelP){
        int numColunas = modelP.getRowCount();

        for (int i = numColunas - 1; i >= 0; i--) {
            modelP.removeRow(i);
        }
        tableP.setModel(modelP);
    }
    
    public void adicionarElementosTabela( JTable tableP, DefaultTableModel modelP) throws SQLException{
        rsmd = rs.getMetaData();
        int numColunas = rsmd.getColumnCount();
          
        Vector<Vector<String>> dadosTabela= new Vector<Vector<String>>();
        Vector<String> colunas = new Vector<String>();
      
        for(int i = 1; i <= numColunas ; i++ ){
            
            colunas.addElement(rsmd.getColumnName(i));
        } 

        while(rs.next()){
            Vector<String>row = new Vector<String>(numColunas);
            for(int i = 1; i <= numColunas; i++){
                row.addElement(rs.getString(i));
            }
            dadosTabela.addElement(row);
        }
            modelP.setDataVector(dadosTabela, colunas);
            tableP.setModel(modelP);
    }
    
    /*Funções de manipulação das listas*/
    public void limparLista(JList listP, DefaultListModel modelP){
        modelP.clear();
        listP.setModel(modelP);
    }
    
    public void adicionarElemento(String text, JList listP, DefaultListModel modelP){
        modelP.addElement(text); 
        listP.setModel(modelP);
    }
    
    public void removerElemento(int index, JList listP, DefaultListModel modelP){
        modelP.remove(index); //retorna a posição do item selecionado.
        listP.setModel(modelP);
    }
    
    public void atualizaLista1() throws SQLException{
        if(tipoBD == 1) 
            rs = stmt.executeQuery("SHOW DATABASES;");
        else if(tipoBD == 2)
            rs = stmt.executeQuery("SELECT datname FROM pg_database;");
    
        limparLista(jList1, modelLista1);
        
        while(rs.next()){
            adicionarElemento(rs.getString(1), jList1, modelLista1);
        }
    }
    
    public void atualizaListasTablesViews() throws SQLException{
        limparLista(jList2, modelLista2);
        limparLista(jList3, modelLista3);
        if(tipoBD == 1)
            rs = stmt.executeQuery("SHOW FULL TABLES WHERE table_type = 'BASE TABLE';");
        else if(tipoBD == 2){
            rs = stmt.executeQuery("SELECT tablename FROM pg_catalog.pg_tables WHERE schemaname != 'pg_catalog' AND schemaname != 'information_schema';");
        }
            
        while(rs.next()){
            adicionarElemento(rs.getString(1), jList2, modelLista2);
        }
        if(tipoBD ==1)
            rs = stmt.executeQuery("SHOW FULL TABLES WHERE table_type = 'VIEW';");
        else if(tipoBD == 2)
            rs = stmt.executeQuery("select " + "table_name as view_name,\n"  +
            "table_schema as schema_name\n" + 
            "from information_schema.views\n" +
            "where table_schema not in ('information_schema', 'pg_catalog')\n" +
            "order by schema_name,\n" +
            "view_name;");

        while(rs.next()){
            adicionarElemento(rs.getString(1), jList3, modelLista3);
        }

        /*OBS: A table_typecolumn(para o MySQL) armazena os tipos de tabelas: 
        BASE TABLE para uma tabela, VIEW para uma visão ou SYSTEM VIEW para 
        uma tabela INFORMATION_SCHEMA.
        FONTE: https://www.mysqltutorial.org/mysql-views/mysql-show-view/*/
    }
    
    public void atualizaPropriedadesTabela(String tabelaSelecionada){
        try {
            rsmd = rs.getMetaData();
            atualizaListaPropriedades(tabelaSelecionada); 
        } catch (SQLException ex) {
            /*Erro ao exibir as propriedades da tabela.*/
            Logger.getLogger(WorkBench.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void atualizaListaPropriedades(String nomeTabela) throws SQLException{
        //propriedades escolhidas
        //primary key, nome dos campos, tipo dos campos, null
         String chave = "";
         ResultSet rsAux;
        
        limparLista(jList4, modelLista4);
        if(tipoBD == 1)
            rs = stmt.executeQuery("describe " + nomeTabela + ";");
        // mudar isso
        else if(tipoBD == 2)
            rs = stmt.executeQuery("select column_name, data_type, is_nullable from INFORMATION_SCHEMA.COLUMNS where table_name = '" + nomeTabela + "';");
            rsAux = stmtAux.executeQuery("SELECT \n" +
                                        "  pg_attribute.attname, \n" +
                                        "  format_type(pg_attribute.atttypid, pg_attribute.atttypmod) \n" +
                                        "FROM pg_index, pg_class, pg_attribute, pg_namespace \n" +
                                        "WHERE \n" +
                                        "  pg_class.oid = '" + nomeTabela + "'::regclass AND \n" +
                                        "  indrelid = pg_class.oid AND \n" +
                                        "  nspname = 'public' AND \n" +
                                        "  pg_class.relnamespace = pg_namespace.oid AND \n" +
                                        "  pg_attribute.attrelid = pg_class.oid AND \n" +
                                        "  pg_attribute.attnum = any(pg_index.indkey)\n" +
                                        " AND indisprimary;");
        while(rs.next()){
            if(tipoBD == 1)
                chave = "PRI".equals(rs.getString(4))? "PRI": "NO"; //Verifica se o campo é chave
            
            else if(tipoBD == 2 ){               
                if(rsAux.next() && rsAux.getString(1).equals(rs.getString(1)) ){
                    chave = "PRI";
                    System.out.println("sou bobo");
                    
                } 
                else{
                    chave = "NO";
                    System.out.println("sou otario");
                }
            }
            
            adicionarElemento(chave + " | " + rs.getString(1) + " | " + rs.getString(2) + " | " + rs.getString(3), jList4, modelLista4);
        }
    }
    
    /*Função para a manipulação do Log*/
    public void exibeLog(String mensagemP){
        textArea2.setText(textArea2.getText() + mensagemP + "\n");
    }

    /*Funções relativas aos comandos SQL(leitura e execução)*/
    public void executaComando(String texto){   //Serve para identificar o tipo de comando, para usar executequery
                                                 //ou usar executeUpdate
        try {
            String textoAux;
            textoAux = texto.toLowerCase();
            
            if((textoAux.indexOf("select") > -1)){ //saber se é select
                if(limite)
                    texto += " limit " + jComboBox1.getItemAt(jComboBox1.getSelectedIndex()) + ";";
            }

            stmt.execute(texto);
            if(stmt.getUpdateCount() < 0){  //se o retorno for menor que 0, nao teve atualizao, logo, executeQuery
                rs = stmt.getResultSet();

                adicionarElementosTabela(jTable2, modelTabela1);
            }
            exibeLog("Instrução executada com sucesso!");
        } catch (SQLException ex) {
            exibeLog(Integer.toString(ex.getErrorCode()) + " - " + ex.getMessage());
        }                                              
    }
      
    public void lerInstrucao(String texto ){  //Essa funcao tem o intuito de identifcar cada instrucao do SQL                             
        String textoComando;
        int tamanhoString = texto.length();
        char textoVetor[] = texto.toCharArray();
        int inicio, fim;
        boolean stringCamposSQL = false;
        inicio = 0; 

        for(int i = 0; (i < tamanhoString) ; i++ ){
           if(textoVetor[i] == '"' || textoVetor[i] == '\'' ){  //saber se esta lendo uma string da instrucao do sql para nao pegar ; desses lugares
               if(stringCamposSQL == false)
                   stringCamposSQL = true;
               else
                   stringCamposSQL = false;    
           }
           
           if((stringCamposSQL == false) && (textoVetor[i] == ';')){
               fim = i;
               textoComando = texto.substring(inicio, fim);
               executaComando(textoComando);
               //executar comando da query
               inicio = i+1; //posicao depois do ';'
           }
        }
    }
    
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jList2 = new javax.swing.JList<>();
        jLabel2 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jList3 = new javax.swing.JList<>();
        jLabel6 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        jList4 = new javax.swing.JList<>();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList<>();
        jLabel4 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jComboBox1 = new javax.swing.JComboBox<>();
        jButton1 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane6 = new javax.swing.JScrollPane();
        textArea1 = new javax.swing.JTextArea();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane7 = new javax.swing.JScrollPane();
        textArea2 = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("SQLBird");

        jPanel1.setPreferredSize(new java.awt.Dimension(1366, 768));

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Bancos de Dados"));
        jPanel2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jPanel2MouseClicked(evt);
            }
        });

        jList2.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jList2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jList2MouseClicked(evt);
            }
        });
        jList2.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jList2KeyReleased(evt);
            }
        });
        jScrollPane2.setViewportView(jList2);

        jLabel2.setText("Bancos");

        jLabel5.setText("Tabelas");

        jList3.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane3.setViewportView(jList3);

        jLabel6.setText("Views");

        jButton2.setLabel("Atualizar");
        jButton2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton2MouseClicked(evt);
            }
        });
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jList4.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jList4.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jList4MouseClicked(evt);
            }
        });
        jScrollPane4.setViewportView(jList4);

        jLabel3.setText("Propriedades da Tabela");

        jList1.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jList1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jList1MouseClicked(evt);
            }
        });
        jList1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jList1KeyReleased(evt);
            }
        });
        jScrollPane1.setViewportView(jList1);

        jLabel4.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        jLabel4.setText("Formato:chave|campo|tipo|nulo");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(79, 79, 79)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel5)
                .addGap(79, 79, 79))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addGap(79, 79, 79))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(jButton2)
                        .addGap(20, 20, 20))))
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap(20, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 160, Short.MAX_VALUE)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(jScrollPane1))
                        .addGap(18, 18, Short.MAX_VALUE)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(20, Short.MAX_VALUE))))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jLabel5))
                .addGap(20, 20, 20)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(20, 20, 20)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel6)
                            .addComponent(jLabel3))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 215, Short.MAX_VALUE)
                            .addComponent(jScrollPane3)))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton2)
                .addGap(20, 20, 20))
        );

        jButton2.getAccessibleContext().setAccessibleName("bAtualizar");

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Comandos SQL"));

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "10", "20", "30", "40", "Sem Limite" }));
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });

        jButton1.setText("Executar");
        jButton1.setActionCommand("bExecutar");
        jButton1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton1MouseClicked(evt);
            }
        });

        jLabel1.setText("Limite de Linhas");

        textArea1.setColumns(20);
        textArea1.setRows(5);
        jScrollPane6.setViewportView(textArea1);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 906, Short.MAX_VALUE)
                        .addGap(20, 20, 20))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jButton1)
                        .addGap(92, 92, 92)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(450, 450, 450))))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addGap(20, 20, 20)
                .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 216, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(24, Short.MAX_VALUE))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Resultados"));

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane5.setViewportView(jTable2);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jScrollPane5)
                .addGap(20, 20, 20))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 137, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(19, Short.MAX_VALUE))
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Log"));
        jPanel5.setToolTipText("");

        textArea2.setEditable(false);
        textArea2.setColumns(20);
        textArea2.setRows(5);
        jScrollPane7.setViewportView(textArea2);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jScrollPane7)
                .addGap(20, 20, 20))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(20, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(20, 20, 20)
                        .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap(55, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    /*Funções para a manipulação de cliques em listas, tabelas, combobox e botões.
    Também existe funções relativas a teclas pressionadas para as listas 1 e 2*/
    private void jPanel2MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel2MouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_jPanel2MouseClicked

    private void jButton1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton1MouseClicked
        String textoSelecionado = textArea1.getSelectedText();
        String textoTotal = textArea1.getText();
        
        if(textoSelecionado != null)
            lerInstrucao(textoSelecionado);
        else
            lerInstrucao(textoTotal);
    }//GEN-LAST:event_jButton1MouseClicked

    private void jButton2MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton2MouseClicked

    }//GEN-LAST:event_jButton2MouseClicked

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        try {
            atualizaLista1();
            atualizaListasTablesViews();
        } catch (SQLException ex) {
            Logger.getLogger(WorkBench.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
         int i = jComboBox1.getSelectedIndex();
         String s = jComboBox1.getItemAt(i);
         
         if(s == "Sem Limite")
             limite = false;
         else
             limite = true;
    }//GEN-LAST:event_jComboBox1ActionPerformed

    private void jList4MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jList4MouseClicked
      
                       
    }//GEN-LAST:event_jList4MouseClicked

    private void jList2MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jList2MouseClicked
        int indice = jList2.getSelectedIndex();
        if(indice != indiceLista2Atual){
            String tabelaSelecionada;
            tabelaSelecionada = modelLista2.getElementAt(indice).toString();
            indiceLista2Atual = indice;

            atualizaPropriedadesTabela(tabelaSelecionada);
        }             
    }//GEN-LAST:event_jList2MouseClicked

    private void jList1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jList1MouseClicked
        int indice = jList1.getSelectedIndex();

        if(indice != indiceLista1Atual){
            String bancoSelecionado;
            String bancoAnterior;
            bancoSelecionado = modelLista1.getElementAt(indice).toString();
            indiceLista1Atual = indice;
            indiceLista2Atual = -1; /*reseta o índice da listas de tabelas(se não resetar da bug nas listas
                de propriedades, já que se selecionar o campo com o mesmo indice na lista
                de tabelas, a lista de propriedades não será atualizada)*/
            try {
                if(tipoBD == 1)
                stmt.executeQuery("USE " + bancoSelecionado + ";");
                else if(tipoBD == 2 ){

                    bancoAnterior = identificaBanco();
                    String urlAux = url.substring(0, url.indexOf(bancoAnterior));
                    con.close();

                    con = DriverManager.getConnection( urlAux + bancoSelecionado, usuario, senha);
                    stmt = con.createStatement();
                    stmtAux = con.createStatement();

                }
                // mudar isso
                atualizaListasTablesViews();
                limparLista(jList4, modelLista4); //Limpa a exibição das prop da tabela, pois mudou de bd.
            } catch (SQLException ex) {
                Logger.getLogger(WorkBench.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jList1MouseClicked

    /*Comando para */
    private void jList1KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jList1KeyReleased
        int indice = jList1.getSelectedIndex();
        
        if(indice != indiceLista1Atual){
            String bancoSelecionado;
            String bancoAnterior;
            bancoSelecionado = modelLista1.getElementAt(indice).toString();
            indiceLista1Atual = indice;
            indiceLista2Atual = -1; /*reseta o índice da listas de tabelas(se não resetar da bug nas listas
                de propriedades, já que se selecionar o campo com o mesmo indice na lista
                de tabelas, a lista de propriedades não será atualizada)*/
            try {
                if(tipoBD == 1)
                stmt.executeQuery("USE " + bancoSelecionado + ";");
                else if(tipoBD == 2 ){

                    bancoAnterior = identificaBanco();
                    String urlAux = url.substring(0, url.indexOf(bancoAnterior));
                    con.close();

                    con = DriverManager.getConnection( urlAux + bancoSelecionado, usuario, senha);
                    stmt = con.createStatement();
                    stmtAux = con.createStatement();

                }
                // mudar isso
                atualizaListasTablesViews();
                limparLista(jList4, modelLista4); //Limpa a exibição das prop da tabela, pois mudou de bd.
            } catch (SQLException ex) {
                Logger.getLogger(WorkBench.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_jList1KeyReleased

    private void jList2KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jList2KeyReleased
        int indice = jList2.getSelectedIndex();
        if(indice != indiceLista2Atual){
            String tabelaSelecionada;
            tabelaSelecionada = modelLista2.getElementAt(indice).toString();
            indiceLista2Atual = indice;

            atualizaPropriedadesTabela(tabelaSelecionada);
        }   
    }//GEN-LAST:event_jList2KeyReleased

    /*Função que identifica o nome do Banco baseado na url da conexão.*/
    public String identificaBanco(){
        int contadorBarra = 0;
        String banco = "";
        char aux[] = url.toCharArray();
        int i;
        for( i = 0; (i < url.length()) && (contadorBarra < 3); i++){
            if(aux[i] == '/'){          
                contadorBarra ++;
            }
        }
        
        banco = url.substring(i);
        return banco;
    }
    
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(WorkBench.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(WorkBench.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(WorkBench.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(WorkBench.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new WorkBench().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JList<String> jList1;
    private javax.swing.JList<String> jList2;
    private javax.swing.JList<String> jList3;
    private javax.swing.JList<String> jList4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JTable jTable2;
    private javax.swing.JTextArea textArea1;
    private javax.swing.JTextArea textArea2;
    // End of variables declaration//GEN-END:variables
}

