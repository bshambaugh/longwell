package edu.mit.simile.longwell.command;

import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;

import edu.mit.simile.longwell.Facade;
import edu.mit.simile.longwell.FacadeStructuredModel;
import edu.mit.simile.longwell.Message;
import edu.mit.simile.longwell.Profile;
import edu.mit.simile.longwell.schema.LearnedClass;
import edu.mit.simile.velocity.InjectionManager;

public class StartCommand extends CommandBase {
    final static private Logger s_logger = Logger.getLogger(StartCommand.class);

    public StartCommand(InjectionManager injectionManager, String template) {
        super(injectionManager, template);
    }

    public void execute(Message msg) throws ServletException {
        if (s_logger.isDebugEnabled()) s_logger.debug("> start");
        Profile profile = msg.getProfile();

        if (profile == null) {
            s_logger.error("Could not retrieve profile: " + msg.m_profileID);
            msg.m_response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            try {
                VelocityContext vcContext = createContext(msg);

                vcContext.put("facades", getFacades(msg));
                vcContext.put("classes", getClasses(msg));

                if (s_logger.isDebugEnabled()) s_logger.debug("> mergeTemplate");
                msg.m_ve.mergeTemplate(m_template, vcContext, msg.m_response.getWriter());
                if (s_logger.isDebugEnabled()) s_logger.debug("< mergeTemplate");
            } catch (Exception e) {
                s_logger.error(e);
                e.printStackTrace();
            }
        }
        if (s_logger.isDebugEnabled()) s_logger.debug("< start");
    }

    protected Set getFacades(Message msg) {
        if (s_logger.isDebugEnabled()) s_logger.debug("> getFacades");

        Comparator comparator = null;
        if ("label".equals(msg.m_query.getFirstParamValue("sortFacades"))) {
            comparator = new Facade.LabelComparator();
        } else {
            comparator = new Facade.CountComparator();
        }

        SortedSet<Facade> facades = new TreeSet<Facade>(comparator);

        FacadeStructuredModel structuredModel = (FacadeStructuredModel) msg.getProfile().getStructuredModel(
                FacadeStructuredModel.class);

        facades.addAll(structuredModel.getFacades());

        if (s_logger.isDebugEnabled()) s_logger.debug("< getFacades");
        return facades;
    }

    protected Set getClasses(Message msg) {
        if (s_logger.isDebugEnabled()) s_logger.debug("> getClasses");

        SortedSet<LearnedClass> sSchemas = new TreeSet<LearnedClass>(new Comparator() {
            String m_locale;

            public boolean equals(Object obj) {
                return false;
            }

            public int compare(Object o1, Object o2) {
                LearnedClass c1 = (LearnedClass) o1;
                LearnedClass c2 = (LearnedClass) o2;

                int i = c2.countItems() - c1.countItems();
                if (i == 0) {
                    i = c1.getLabel(m_locale).compareToIgnoreCase(c2.getLabel(m_locale));
                }
                if (i == 0) {
                    i = c1.getURI().toString().compareTo(c2.getURI().toString());
                }
                return i;
            }

            public Comparator init(String locale) {
                m_locale = locale;
                return this;
            }
        }.init(msg.m_locale));

        sSchemas.addAll(msg.getProfile().getSchemaModel().getLearnedClasses());

        if (s_logger.isDebugEnabled()) s_logger.debug("< getClasses");
        return sSchemas;
    }
}
