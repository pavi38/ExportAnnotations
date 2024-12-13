import qupath.lib.roi.RectangleROI
import qupath.imagej.gui.IJExtension
import ij.IJ
import ij.gui.Roi
import ij.plugin.ChannelSplitter
import ij.plugin.frame.RoiManager

import qupath.lib.images.servers.ImageServer
import qupath.lib.objects.PathObject
import javax.imageio.ImageIO
import java.awt.image.BufferedImage

def channel_of_interest = 1 // null to export all the channels 
def downsample = 1 // Value of 1 corresponds to no downsampling

// Get the main QuPath data structures
def imageData = getCurrentImageData()
def hierarchy = imageData.getHierarchy()
def server = imageData.getServer()

// Get the annotations
def annotations = hierarchy.getAnnotationObjects()

// Define the region request for the full image
def request = RegionRequest.createInstance(
    server.getPath(),   // Path to the image server
    downsample,               
    0, 0,               // Top-left corner (x, y)
    server.getWidth(),  // Image width
    server.getHeight()  // Image height
)

// Create a BufferedImage for the original image
def img = server.readBufferedImage(request)

// Create a BufferedImage for the mask
def mask = new BufferedImage(server.getWidth(), server.getHeight(), BufferedImage.TYPE_BYTE_BINARY)
def maskGraphics = mask.createGraphics()

// Create a graphics context to draw on the image
def graphics = img.createGraphics()

graphics.setStroke(new java.awt.BasicStroke(3)) // Set line thickness

// Name of the output image
def imag_name = server.getMetadata().getName()

// Draw annotations with different colors based on their classification
for (annotation in annotations) {
    def roi = annotation.getROI()
    def shape = roi.getShape()

    // Retrieve the color assigned in QuPath for this classification
    def pathClass = annotation.getPathClass()
    def colorInt = pathClass?.getColor() ?: 0xFF000000 // Use classification color or default to black
    def color = new java.awt.Color((colorInt >> 16) & 0xFF, (colorInt >> 8) & 0xFF, colorInt & 0xFF)

    graphics.setColor(color)
    graphics.draw(shape)

    // Draw on the mask (all annotations as white)
    maskGraphics.setColor(java.awt.Color.WHITE)
    maskGraphics.fill(shape)
}

// Clean up
graphics.dispose()
maskGraphics.dispose()

// Save the images and mask
saveImages(img, mask, imag_name)

// This will save the images in the current QuPath project folder
def saveImages(def images, def labels, def name) {
    def source_folder = new File(buildFilePath(PROJECT_BASE_DIR, 'ground_truth', 'images'))
    def target_folder = new File(buildFilePath(PROJECT_BASE_DIR, 'ground_truth', 'masks'))
    mkdirs(source_folder.getAbsolutePath())
    mkdirs(target_folder.getAbsolutePath())

    //output file paths
    def image_file = new File(source_folder, name + '.png')
    def label_file = new File(target_folder, name + '_mask.png')

    ImageIO.write(images, "PNG", image_file)
    ImageIO.write(labels, "PNG", label_file)
}

print "done"
