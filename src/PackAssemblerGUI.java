import javax.swing.*;


public class PackAssemblerGUI
{

	private JFrame frame;
	private JTextArea textArea;
	private JScrollPane scrollPane;

	/**
	 * Create the application.
	 */
	public PackAssemblerGUI()
	{
		initialize();
	}

	public JFrame getFrame()
	{
		return frame;
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize()
	{
		frame = new JFrame();
		frame.setBounds( 100, 100, 648, 456 );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		scrollPane = new JScrollPane();
		GroupLayout groupLayout = new GroupLayout( frame.getContentPane() );
		groupLayout.setHorizontalGroup(
				groupLayout.createParallelGroup( GroupLayout.Alignment.TRAILING )
						.addGroup( GroupLayout.Alignment.LEADING, groupLayout.createSequentialGroup()
								.addGap( 12 )
								.addComponent( scrollPane, GroupLayout.PREFERRED_SIZE, 608, GroupLayout.PREFERRED_SIZE )
								.addContainerGap( 403, Short.MAX_VALUE ) )
		);
		groupLayout.setVerticalGroup(
				groupLayout.createParallelGroup( GroupLayout.Alignment.TRAILING )
						.addGroup( groupLayout.createSequentialGroup()
								.addContainerGap()
								.addComponent( scrollPane, GroupLayout.DEFAULT_SIZE, 396, Short.MAX_VALUE )
								.addContainerGap() )
		);

		textArea = new JTextArea();
		scrollPane.setViewportView( textArea );
		textArea.setLineWrap( true );
		textArea.setEditable( false );
		frame.getContentPane().setLayout( groupLayout );
	}

	public void addString( String line )
	{
		textArea.append( line );
		updateScrollArea();
	}

	public void addLine( String line )
	{
		textArea.append( line + "\n" );
		updateScrollArea();
	}

	private void updateScrollArea()
	{
		textArea.setCaretPosition( textArea.getDocument().getLength() );
	}
}
