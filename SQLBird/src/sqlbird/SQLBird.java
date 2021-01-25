/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sqlbird;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 Trabalho desenvolvido para a disciplina de Introdução a Banco de Dados - S71
 Professor: Leandro Batista de Almeida 
 Alunos: João Victor Laskoski e Luis Camilo Jussiani Moreira
 */
public class SQLBird {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
       java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    new Conexoes().setVisible(true);
                } catch (SQLException ex) {
                    Logger.getLogger(SQLBird.class.getName()).log(Level.SEVERE, null, ex);
                }
                
            }
        });
    
    }   

}
