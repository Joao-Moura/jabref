package org.jabref.logic.openoffice.style;
import com.sun.star.text.XTextCursor;
import org.jabref.logic.openoffice.backend.GetContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CursorStringContextTest {
    private XTextCursor cursorStringMock;
    private int charBefore;
    private int charAfter;
    private Boolean htmlMarkup;

    @BeforeEach
    public void setUp(){
        cursorStringMock = mock(XTextCursor.class);
    }

    @Test
    public void testAddReferenceWithHtml(){
        // (1) Caso de teste válido, em que para a string passada é retornado seu contexto
        // Em específico, nesse teste a variável htmlMarkup é passada para adicionar as tags <b>
        charBefore = 2;
        charAfter = 3;
        htmlMarkup = true;

        when(cursorStringMock.getString()).thenReturn("Tes te");
        String context = GetContext.getCursorStringWithContext(
                cursorStringMock, charBefore, charAfter, htmlMarkup);
        assertEquals(context, "<b>Tes te</b>");
    }

    @Test
    public void testAddReferenceWithoutHtml(){
        // (4) Caso similar ao anterior, porém nesse a variável de html é passada
        // como falsa, sendo o resultado o esperado na especificação.
        charBefore = 8;
        charAfter = 3;
        htmlMarkup = false;
        String returnValue = new String("12345678");

        when(cursorStringMock.getString()).thenReturn(returnValue);
        String context = GetContext.getCursorStringWithContext(
                cursorStringMock, charBefore, charAfter, htmlMarkup);
        assertEquals(context, returnValue);
    }

    @Test
    public void throwsIndexOutOfBoundsExceptionLeft() {
        // (2) Caso de teste "inválido", que pega o contexto porém uma exceção é levantada
        // no logger e disponível ao final da execução do teste. Essa exceção em específico
        // ocorre dentro do primeiro loop, onde o valor é inválido.
        charBefore = 1;
        charAfter = 0;
        htmlMarkup = false;
        String returnValue = new String("");

        when(cursorStringMock.getString()).thenReturn(returnValue);
        String context = GetContext.getCursorStringWithContext(
                cursorStringMock, charBefore, charAfter, htmlMarkup);
        assertEquals(context, returnValue);
    }

    @Test
    public void throwsIndexOutOfBoundsExceptionRight(){
        // (3) Similar ao anterior, caso de teste "inválido" que levanta a exceção
        // capturada no logger. Dessa vez, ao contrário do teste anterior, o
        // erro acontece dentro do segundo loop do método principal.
        charBefore = 0;
        charAfter = 1;
        htmlMarkup = false;
        String returnValue = new String("");

        when(cursorStringMock.getString()).thenReturn(returnValue);
        String context = GetContext.getCursorStringWithContext(
                cursorStringMock, charBefore, charAfter, htmlMarkup);
        assertEquals(context, returnValue);
    }
}
