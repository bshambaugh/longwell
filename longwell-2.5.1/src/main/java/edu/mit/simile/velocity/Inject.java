package edu.mit.simile.velocity;

import java.io.IOException;
import java.io.Writer;

import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.directive.InputBase;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.runtime.parser.node.SimpleNode;

public class Inject extends InputBase {

    final static private Logger s_logger = Logger.getLogger(Inject.class);
    
    public String getName() {
        return "inject";
    }

    public int getType() {
        return LINE;
    }

    public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException,
            ResourceNotFoundException, ParseErrorException, MethodInvocationException {

        InjectionManager im = (InjectionManager) context.get("injections");
        
        if (node == null || node.jjtGetNumChildren() == 0) {
            // if #inject is called with no value, inject the supertype
            String template = context.getCurrentTemplateName();

            if (s_logger.isDebugEnabled()) s_logger.debug("Injecting parent of " + template);
            
            if (im != null && im.hasParentInjection(template)) {
                return inject(im.getParentInjection(template), context, writer);
            } else {
                return false;
            }
        } else {
            Object value = node.jjtGetChild(0).value(context);

            if (value == null) {
                rsvc.error("#inject() error:  null argument");
                return false;
            }

            String name = value.toString();

            if (s_logger.isDebugEnabled()) s_logger.debug("Injecting " + name);

            if (im != null && im.hasInjection(name)) {
                return inject(im.getInjection(name), context, writer);
            } else {
                return false;
            }
        }
    }

    private boolean inject(String template, InternalContextAdapter context, Writer writer)
            throws ResourceNotFoundException, ParseErrorException, MethodInvocationException {

        // see if we have exceeded the configured depth.
        // If it isn't configured, put a stop at 20 just in case.

        Object[] templateStack = context.getTemplateNameStack();

        if (templateStack.length >= rsvc.getInt(RuntimeConstants.PARSE_DIRECTIVE_MAXDEPTH, 20)) {
            StringBuffer path = new StringBuffer();

            for (int i = 0; i < templateStack.length; ++i) {
                path.append(" > " + templateStack[i]);
            }

            rsvc.error("Max recursion depth reached (" + templateStack.length + ")" + " File stack:" + path);
            return false;
        }

        //  now use the Runtime resource loader to get the template

        Template t = null;

        try {
            t = rsvc.getTemplate(template, getInputEncoding(context));
        } catch (ResourceNotFoundException rnfe) {
            // the arg wasn't found.  Note it and throw
            rsvc.error("#inject(): cannot find template '" + template + "', called from template "
                    + context.getCurrentTemplateName() + " at (" + getLine() + ", " + getColumn() + ")");
            throw rnfe;
        } catch (ParseErrorException pee) {
            // the arg was found, but didn't parse - syntax error
            rsvc.error("#inject(): syntax error in #inject()-ed template '" + template + "', called from template "
                    + context.getCurrentTemplateName() + " at (" + getLine() + ", " + getColumn() + ")");
            throw pee;
        } catch (Exception e) {
            rsvc.error("#inject(): arg = " + template + ".  Exception: " + e);
            return false;
        }

        //  and render it
        try {
            context.pushCurrentTemplateName(template);
            ((SimpleNode) t.getData()).render(context, writer);
        } catch (Exception e) {
            //  if it's a MIE, it came from the render.... throw it...
            if (e instanceof MethodInvocationException) {
                throw (MethodInvocationException) e;
            }
            rsvc.error("Exception rendering #inject(" + template + "): " + e);
            s_logger.error("Exception rendering #inject(" + template + ")", e);
            return false;
        } finally {
            context.popCurrentTemplateName();
        }

        return true;
    }
}
