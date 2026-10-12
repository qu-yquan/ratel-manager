import { Image } from 'antd';
import type { BaseComponentProps } from '@json-render/react';
import { KnowledgeCitationText } from '@/components/KnowledgeCitation';
import styles from './ImageGallery.module.less';

interface ImageItem {
  url: string;
  title?: string | null;
  description?: string | null;
  alt?: string | null;
}

interface ImageGalleryProps {
  title?: string | null;
  description?: string | null;
  layout?: 'grid' | 'carousel' | null;
  images: ImageItem[];
}

const ImageGallery: React.FC<BaseComponentProps<ImageGalleryProps>> = ({
  props,
}) => {
  const images = props.images ?? [];

  if (images.length === 0) {
    return null;
  }

  return (
    <div className={styles.gallery}>
      {props.title && (
        <div className={styles.title}>
          <KnowledgeCitationText text={props.title} />
        </div>
      )}
      {props.description && (
        <div className={styles.description}>
          <KnowledgeCitationText text={props.description} />
        </div>
      )}
      <Image.PreviewGroup>
        <div
          className={styles.grid}
          data-layout={props.layout === 'carousel' ? 'carousel' : 'grid'}
        >
          {images.map((image, index) => (
            <figure key={`${image.url}-${index}`} className={styles.card}>
              <Image
                className={styles.image}
                src={image.url}
                alt={image.alt || image.title || `image-${index + 1}`}
              />
              {(image.title || image.description) && (
                <figcaption className={styles.caption}>
                  {image.title && (
                    <div className={styles.imageTitle}>
                      <KnowledgeCitationText text={image.title} />
                    </div>
                  )}
                  {image.description && (
                    <div className={styles.imageDescription}>
                      <KnowledgeCitationText text={image.description} />
                    </div>
                  )}
                </figcaption>
              )}
            </figure>
          ))}
        </div>
      </Image.PreviewGroup>
    </div>
  );
};

export default ImageGallery;
