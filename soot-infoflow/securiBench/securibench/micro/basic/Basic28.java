/**
   @author Benjamin Livshits <livshits@cs.stanford.edu>
   
   $Id: Basic28.java,v 1.4 2006/04/04 20:00:40 livshits Exp $
 */
package securibench.micro.basic;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="complicated control flow"
 * @servlet vuln_count = "2"
 */
public class Basic28 extends BasicTestCase implements MicroTestCase {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String name = req.getParameter("name");
        boolean b[] = new boolean[3];
        PrintWriter writer = resp.getWriter();
        if (b[0]) {
            if (b[0]) {
                if (b[0]) {
                    if (b[0]) {
                        if (b[0]) {
                            if (b[0]) {
                                if (b[0]) {
                                    if (b[0]) {
                                        if (b[0]) {
                                        }
                                    } else {
                                    }
                                } else {
                                }
                                if (b[0]) {
                                }
                            } else {
                            }
                        } else {
                            if (b[0]) {
                                if (b[0]) {
                                    if (b[0]) {
                                    }
                                } else {
                                }
                            } else {
                            }
                        }
                    } else {
                        if (b[0]) {
                            if (b[0]) {
                                if (b[0]) {
                                }
                                writer.println(name);       /* BAD */
                            } else {
                            }
                        } else {
                        }
                    }
                } else {
                    if (b[0]) {
                        if (b[0]) {
                            if (b[0]) {
                            }
                        } else {
                        }
                    } else {
                    }
                }
            } else {
                if (b[0]) {
                    if (b[0]) {
                        if (b[0]) {
                            if (b[0]) {
                                if (b[0]) {
                                }
                            } else {
                            }
                        } else {
                            if (b[0]) {
                                if (b[0]) {
                                    if (b[0]) {
                                    }
                                } else {
                                }
                            } else {
                            }
                        }
                    } else {
                        if (b[0]) {
                            if (b[0]) {
                                if (b[0]) {
                                }
                            } else {
                            }
                        } else {
                        }
                    }
                } else {
                    if (b[0]) {
                        if (b[0]) {
                            if (b[0]) {
                            }
                        } else {
                        }
                    } else {
                        if (b[0]) {
                            if (b[0]) {
                                if (b[0]) {
                                }
                            } else {
                            }
                        } else {
                        }
                    }
                }
            }
        } else {
            if (b[0]) {
                if (b[0]) {
                    if (b[0]) {
                        writer.println(name);       /* BAD */
                    }
                } else {
                }
            } else {
            }
        }
    }

    public String getDescription() {
        return "complicated control flow";
    }

    public int getVulnerabilityCount() {
        return 2;
    }
}